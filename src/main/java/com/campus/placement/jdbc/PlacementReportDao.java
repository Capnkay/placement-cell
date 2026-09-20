package com.campus.placement.jdbc;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * The JDBC half of the application.
 *
 * <p>Everything else here talks to the database through JPA and Hibernate,
 * which is the right tool when you are loading an object, changing a field and
 * saving it back. Reports are the opposite shape of problem: they aggregate
 * across several tables, return numbers rather than objects, and are never
 * written back. Expressing that as JPQL fights the mapping, so these queries are
 * written as SQL and read with plain JDBC.</p>
 *
 * <h2>The five steps, and where each one is</h2>
 * <ol>
 *   <li><b>Get a datasource.</b> Not with {@code DriverManager} and a hard coded
 *       URL: the pool is looked up in the naming service by the name that
 *       {@code WEB-INF/glassfish-resources.xml} published. See
 *       {@link #dataSource()}.</li>
 *   <li><b>Get a connection</b> from that pool with {@code getConnection()}.</li>
 *   <li><b>Prepare a statement.</b> Always a {@link PreparedStatement}, never a
 *       string built by concatenation, so a value typed by a user can never be
 *       read as SQL.</li>
 *   <li><b>Read the {@link ResultSet}</b> column by column into a plain object.</li>
 *   <li><b>Release everything.</b> Every connection, statement and result set is
 *       opened in a try with resources block, so they are closed in reverse
 *       order even when a query throws. A connection that is not closed is not
 *       destroyed, it is never handed back to the pool, and enough of those will
 *       stall the whole application.</li>
 * </ol>
 *
 * <p>The bean is marked {@code NOT_SUPPORTED} so the container does not start a
 * JTA transaction around these calls. That matters for
 * {@link #closeDriveAndRejectPending(long)}: JDBC transaction control with
 * {@code setAutoCommit(false)} and {@code commit()} is only meaningful when the
 * container is not already managing a transaction of its own.</p>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PlacementReportDao {

    private static final Logger LOG = Logger.getLogger(PlacementReportDao.class.getName());

    /**
     * The name published by WEB-INF/glassfish-resources.xml. The java:app prefix
     * scopes it to this application, so the resource lives and dies with the
     * deployment instead of sitting in the server's global namespace.
     */
    public static final String JNDI_NAME = "java:app/jdbc/placementDS";

    /**
     * Injected by the container. The annotation is doing a JNDI lookup on your
     * behalf, which is why {@link #lookupDataSource()} exists beside it: same
     * result, written out by hand.
     */
    @Resource(lookup = JNDI_NAME)
    private DataSource dataSource;

    private DataSource dataSource() throws SQLException {
        if (dataSource != null) {
            return dataSource;
        }
        try {
            return lookupDataSource();
        } catch (NamingException ex) {
            throw new SQLException("The datasource " + JNDI_NAME + " is not bound", ex);
        }
    }

    /**
     * The explicit form of what {@code @Resource} does. Kept because it is worth
     * being able to point at the actual lookup rather than an annotation.
     */
    public DataSource lookupDataSource() throws NamingException {
        return (DataSource) new InitialContext().lookup(JNDI_NAME);
    }

    /**
     * Asks the live connection to describe itself. Nothing here is stored in the
     * application: the driver, the server version and the URL all come from
     * {@link DatabaseMetaData}.
     */
    public ConnectionInfo describeConnection() {
        ConnectionInfo info = new ConnectionInfo();
        try (Connection connection = dataSource().getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            info.setAvailable(true);
            info.put("JNDI name", JNDI_NAME);
            info.put("Database", meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            info.put("JDBC driver", meta.getDriverName() + " " + meta.getDriverVersion());
            info.put("JDBC URL", meta.getURL());
            info.put("Connected as", meta.getUserName());
            info.put("JDBC specification", meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
            info.put("Auto commit", String.valueOf(connection.getAutoCommit()));
            info.put("Read only", String.valueOf(connection.isReadOnly()));
            info.put("Transaction isolation", isolationName(connection.getTransactionIsolation()));
            info.put("Connection class", connection.getClass().getName());
        } catch (SQLException ex) {
            info.setAvailable(false);
            info.setFailure(ex.getMessage());
            LOG.log(Level.WARNING, "Could not describe the JDBC connection", ex);
        }
        return info;
    }

    private String isolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            case Connection.TRANSACTION_NONE -> "NONE";
            default -> "unknown (" + level + ")";
        };
    }

    // ------------------------------------------------------------------
    // Report one: how each branch is doing
    // ------------------------------------------------------------------

    public static final String BRANCH_SQL = """
            SELECT s.branch AS branch,
                   COUNT(DISTINCT s.id) AS students,
                   COUNT(a.id) AS applications,
                   COUNT(CASE WHEN a.status IN ('SHORTLISTED','INTERVIEW','SELECTED')
                              THEN 1 END) AS shortlisted,
                   COUNT(DISTINCT CASE WHEN a.status = 'SELECTED'
                              THEN s.id END) AS selected,
                   AVG(s.cgpa) AS avg_cgpa,
                   COALESCE(MAX(CASE WHEN a.status = 'SELECTED'
                              THEN d.package_lpa END), 0) AS best_package
              FROM student_profile s
              LEFT JOIN job_application a ON a.student_id = s.id
              LEFT JOIN drive d ON d.id = a.drive_id
             GROUP BY s.branch
             ORDER BY selected DESC, s.branch
            """;

    /**
     * A LEFT JOIN so a branch with no applications still appears with zeroes,
     * which an INNER JOIN would silently drop. That is the sort of thing the ORM
     * would hide and the report would be quietly wrong.
     */
    public List<BranchReport> branchReport() throws SQLException {
        List<BranchReport> rows = new ArrayList<>();
        try (Connection connection = dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(BRANCH_SQL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                BranchReport row = new BranchReport();
                row.setBranch(rs.getString("branch"));
                row.setStudents(rs.getInt("students"));
                row.setApplications(rs.getInt("applications"));
                row.setShortlisted(rs.getInt("shortlisted"));
                row.setSelected(rs.getInt("selected"));
                row.setAverageCgpa(rs.getDouble("avg_cgpa"));
                row.setBestPackage(rs.getDouble("best_package"));
                rows.add(row);
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Report two: how each recruiter is doing
    // ------------------------------------------------------------------

    public static final String COMPANY_SQL = """
            SELECT c.name AS company,
                   c.sector AS sector,
                   (SELECT COUNT(*) FROM drive d
                     WHERE d.company_id = c.id) AS drives,
                   (SELECT COUNT(*) FROM job_application a
                      JOIN drive d ON d.id = a.drive_id
                     WHERE d.company_id = c.id) AS applications,
                   (SELECT COUNT(*) FROM job_application a
                      JOIN drive d ON d.id = a.drive_id
                     WHERE d.company_id = c.id
                       AND a.status = 'SELECTED') AS selected,
                   (SELECT COALESCE(AVG(d.package_lpa), 0) FROM drive d
                     WHERE d.company_id = c.id) AS avg_package,
                   (SELECT COALESCE(MAX(d.package_lpa), 0) FROM drive d
                     WHERE d.company_id = c.id) AS best_package
              FROM company c
             ORDER BY selected DESC, c.name
            """;

    /**
     * Written with correlated subqueries rather than one big join on purpose.
     * Joining drives and applications together and then averaging the package
     * would count each drive once per application and quietly report the wrong
     * average, which is the classic aggregate bug.
     */
    public List<CompanyReport> companyReport() throws SQLException {
        List<CompanyReport> rows = new ArrayList<>();
        try (Connection connection = dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(COMPANY_SQL);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                CompanyReport row = new CompanyReport();
                row.setCompany(rs.getString("company"));
                row.setSector(rs.getString("sector"));
                row.setDrives(rs.getInt("drives"));
                row.setApplications(rs.getInt("applications"));
                row.setSelected(rs.getInt("selected"));
                row.setAveragePackage(rs.getDouble("avg_package"));
                row.setBestPackage(rs.getDouble("best_package"));
                rows.add(row);
            }
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Filtered search: a parameterised statement built at runtime
    // ------------------------------------------------------------------

    /**
     * Finds candidates matching whatever the officer filled in.
     *
     * <p>The interesting part is how the optional filters are added. The
     * conditions are appended to the SQL as {@code ?} placeholders and the
     * values are collected into a list in the same order, then bound by index.
     * The SQL text never contains anything the user typed, so a branch named
     * {@code ' OR 1=1 --} is searched for as a literal branch name and finds
     * nothing, instead of returning the whole table.</p>
     *
     * @param branch      exact branch, or null for every branch
     * @param minCgpa     lowest acceptable CGPA, or null for no floor
     * @param maxBacklogs highest acceptable backlog count, or null for no cap
     * @param onlyUnplaced when true, students who already hold an offer are hidden
     */
    public List<CandidateRow> searchCandidates(String branch, Double minCgpa,
                                               Integer maxBacklogs, boolean onlyUnplaced)
            throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT s.id AS profile_id, s.roll_no, s.branch, s.cgpa, s.backlogs,
                       u.full_name, u.email,
                       (SELECT COUNT(*) FROM job_application a
                         WHERE a.student_id = s.id) AS applications,
                       (SELECT COUNT(*) FROM job_application a
                         WHERE a.student_id = s.id
                           AND a.status = 'SELECTED') AS offers
                  FROM student_profile s
                  JOIN app_user u ON u.id = s.user_id
                 WHERE 1 = 1
                """);

        List<Object> parameters = new ArrayList<>();

        if (branch != null && !branch.isBlank()) {
            sql.append(" AND s.branch = ? ");
            parameters.add(branch);
        }
        if (minCgpa != null) {
            sql.append(" AND s.cgpa >= ? ");
            parameters.add(minCgpa);
        }
        if (maxBacklogs != null) {
            sql.append(" AND s.backlogs <= ? ");
            parameters.add(maxBacklogs);
        }
        if (onlyUnplaced) {
            sql.append("""
                     AND NOT EXISTS (SELECT 1 FROM job_application a
                                      WHERE a.student_id = s.id
                                        AND a.status = 'SELECTED')
                    """);
        }
        sql.append(" ORDER BY s.cgpa DESC, s.roll_no ");

        List<CandidateRow> rows = new ArrayList<>();
        try (Connection connection = dataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            // Bound by position, one placeholder at a time, in the order added.
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    CandidateRow row = new CandidateRow();
                    row.setProfileId(rs.getLong("profile_id"));
                    row.setRollNo(rs.getString("roll_no"));
                    row.setFullName(rs.getString("full_name"));
                    row.setEmail(rs.getString("email"));
                    row.setBranch(rs.getString("branch"));
                    row.setCgpa(rs.getDouble("cgpa"));
                    row.setBacklogs(rs.getInt("backlogs"));
                    row.setApplications(rs.getInt("applications"));
                    row.setPlaced(rs.getInt("offers") > 0);
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /** The statement the last search would run, shown on the page for reference. */
    public String explainSearch(String branch, Double minCgpa, Integer maxBacklogs,
                                boolean onlyUnplaced) {
        List<String> clauses = new ArrayList<>();
        if (branch != null && !branch.isBlank()) {
            clauses.add("s.branch = ?");
        }
        if (minCgpa != null) {
            clauses.add("s.cgpa >= ?");
        }
        if (maxBacklogs != null) {
            clauses.add("s.backlogs <= ?");
        }
        if (onlyUnplaced) {
            clauses.add("NOT EXISTS (offer for this student)");
        }
        return clauses.isEmpty() ? "no filters applied" : String.join(" AND ", clauses);
    }

    // ------------------------------------------------------------------
    // A transaction driven by hand
    // ------------------------------------------------------------------

    /**
     * Closes a drive and rejects everything still sitting at the applied stage,
     * as one unit of work.
     *
     * <p>This is the transaction demonstration. Two statements have to succeed
     * together: a drive that is closed while its pending applications are left
     * hanging is a worse state than either change on its own. So auto commit is
     * switched off, both updates run, and {@code commit()} makes them visible at
     * the same instant. If the second one throws, {@code rollback()} undoes the
     * first as well and the database looks untouched.</p>
     *
     * <p>The {@code finally} block restores auto commit before the connection
     * goes back to the pool. Skipping that hands the next borrower a connection
     * with auto commit still off, and their next write would sit uncommitted
     * until something else happened to commit it. That is a genuinely nasty bug
     * to chase, and it is why the restore is not optional.</p>
     *
     * @return how many pending applications were rejected
     */
    public int closeDriveAndRejectPending(long driveId) throws SQLException {
        String closeDrive = "UPDATE drive SET status = 'CLOSED' WHERE id = ?";
        String rejectPending = """
                UPDATE job_application
                   SET status = 'REJECTED',
                       remarks = 'Drive closed by the placement cell',
                       updated_at = ?
                 WHERE drive_id = ?
                   AND status = 'APPLIED'
                """;

        Connection connection = null;
        boolean previousAutoCommit = true;
        try {
            connection = dataSource().getConnection();
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement close = connection.prepareStatement(closeDrive)) {
                close.setLong(1, driveId);
                close.executeUpdate();
            }

            int rejected;
            try (PreparedStatement reject = connection.prepareStatement(rejectPending)) {
                reject.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                reject.setLong(2, driveId);
                rejected = reject.executeUpdate();
            }

            connection.commit();
            return rejected;

        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                    LOG.warning("Rolled back the close of drive " + driveId + ": " + ex.getMessage());
                } catch (SQLException rollbackFailure) {
                    LOG.log(Level.SEVERE, "The rollback itself failed", rollbackFailure);
                }
            }
            throw ex;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Nothing useful to do, the pool will discard a broken connection.
                }
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // Already gone.
                }
            }
        }
    }

    /**
     * Counts the rows in a table using a plain {@link Statement}, purely to have
     * one example of the simpler API beside the prepared ones. It is safe here
     * only because the table name is chosen from a fixed list, never from input.
     */
    public long countRows(String table) throws SQLException {
        List<String> allowed = List.of("app_user", "student_profile", "company",
                "drive", "job_application", "audit_log");
        if (!allowed.contains(table)) {
            throw new IllegalArgumentException("Not a table this method will read: " + table);
        }
        try (Connection connection = dataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
