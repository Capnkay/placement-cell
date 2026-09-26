package com.campus.placement.ejb;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Records a student's mock interview scores, written the way the practical
 * sessions write database code: a stateful bean that loads the driver by name,
 * opens its own connection with {@code DriverManager} and runs a {@code Statement}.
 * It is the same shape as {@code MarksEntryBean} from the marks entry practical,
 * pointed at MySQL instead of Derby.
 *
 * <p>It sits beside the real data layer on purpose, so both can be shown side by
 * side. {@link #addScore} is the classroom version. {@link #addScoreSafely} does the
 * same job the way the rest of this project does it. What separates them:</p>
 *
 * <ul>
 *   <li>the classroom version opens a new TCP connection on every call, has the URL,
 *       user and password written in the source, and never closes the connection;</li>
 *   <li>it builds the SQL by joining strings, so a value containing a quote changes
 *       the statement itself, which is SQL injection;</li>
 *   <li>the safe version borrows from the same connection pool as everything else,
 *       binds values with {@code ?} placeholders and closes what it opens.</li>
 * </ul>
 *
 * <p>Because the classroom version is unsafe by design, the servlet in front of it
 * checks every value first. The bean stays as it is so the code can be read and
 * compared, but nothing unchecked ever reaches it from a browser.</p>
 */
@Stateful
public class MockScoreBean {

    /** The classroom style: everything the connection needs, written into the code. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/placement_cell?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "placement_user";
    private static final String PASSWORD = "Placement@2026";

    /** One scored mock interview, read back from the table. */
    public record Score(long id, String studentName, int aptitude, int technical, int interview) {
        public int total() {
            return aptitude + technical + interview;
        }
    }

    /**
     * Classroom style. Compare with {@code MarksEntryBean.addMarks}, which is
     * {@code void} and only prints a failure. This one reports how many rows it
     * saved, so the page can tell the truth: a name with an apostrophe, such as
     * O'Brien, ends the SQL string early, the statement is rejected, and the
     * result is 0. That failure is the everyday face of SQL injection.
     *
     * @return the number of rows inserted, 0 when the statement failed
     */
    public int addScore(String sname, int m1, int m2, int m3) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement st = con.createStatement();
            st.executeUpdate("create table if not exists mock_score("
                    + "id bigint auto_increment primary key, sname varchar(120) not null, "
                    + "aptitude int not null, technical int not null, interview int not null)");
            String query = "insert into mock_score(sname,aptitude,technical,interview) values('" + sname + "'  ,"
                    + m1 + "  ," + m2 + "  ," + m3 + ")";
            return st.executeUpdate(query);
        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
    }

    /** The same insert done properly: pooled connection, placeholders, everything closed. */
    public void addScoreSafely(String sname, int m1, int m2, int m3) throws SQLException {
        try (Connection con = pooled();
             PreparedStatement ps = con.prepareStatement(
                     "insert into mock_score(sname,aptitude,technical,interview) values(?,?,?,?)")) {
            ps.setString(1, sname);
            ps.setInt(2, m1);
            ps.setInt(3, m2);
            ps.setInt(4, m3);
            ps.executeUpdate();
        }
    }

    /** Reads the table back with a plain {@code Statement} and {@code ResultSet}. */
    public List<Score> latest() throws SQLException {
        List<Score> scores = new ArrayList<>();
        try (Connection con = pooled();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "select id,sname,aptitude,technical,interview from mock_score order by id desc limit 10")) {
            while (rs.next()) {
                scores.add(new Score(rs.getLong("id"), rs.getString("sname"),
                        rs.getInt("aptitude"), rs.getInt("technical"), rs.getInt("interview")));
            }
        } catch (SQLException missing) {
            // The table is created by the first classroom style insert. Before that
            // there is simply nothing to show.
            if (!"42S02".equals(missing.getSQLState())) {
                throw missing;
            }
        }
        return scores;
    }

    /**
     * Ends the conversation. A stateful bean lives until it is removed or times
     * out, so a caller that only needs it once says so, exactly as the shortlist
     * bean's {@code finish} does.
     */
    @Remove
    public void finished() {
        // Nothing to release: each method above opens and closes its own connection.
    }

    private Connection pooled() throws SQLException {
        try {
            javax.sql.DataSource ds = (javax.sql.DataSource)
                    new javax.naming.InitialContext().lookup("java:app/jdbc/placementDS");
            return ds.getConnection();
        } catch (javax.naming.NamingException ex) {
            throw new SQLException("The placementDS datasource is not bound", ex);
        }
    }
}
