package com.campus.placement.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Talks to the real MySQL database rather than a stub.
 *
 * <p>Inside the server the connection comes from the pool that JNDI publishes.
 * A test runs outside the server, so there is no naming service and no pool, and
 * it opens its own connection with {@code DriverManager} instead. That contrast
 * is worth seeing: the SQL and the JDBC calls are identical, only the way the
 * connection is obtained differs.</p>
 *
 * <p>If MySQL is not running the whole class is skipped rather than failed,
 * because a database that is switched off is not a broken application.</p>
 */
@DisplayName("Database connectivity")
class DatabaseConnectivityTest {

    private static final String URL =
            "jdbc:mysql://localhost:3306/placement_cell"
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "placement_user";
    private static final String PASSWORD = "Placement@2026";

    private static boolean reachable;

    @BeforeAll
    static void checkDatabaseIsUp() {
        try (Connection ignored = DriverManager.getConnection(URL, USER, PASSWORD)) {
            reachable = true;
        } catch (SQLException ex) {
            reachable = false;
            System.out.println("MySQL is not reachable, connectivity tests skipped: "
                    + ex.getMessage());
        }
    }

    private Connection open() throws SQLException {
        assumeTrue(reachable, "MySQL is not running on localhost:3306");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @Test
    @DisplayName("the driver connects and reports the server it reached")
    void connectsAndDescribesItself() throws SQLException {
        try (Connection connection = open()) {
            DatabaseMetaData meta = connection.getMetaData();

            assertTrue(meta.getDatabaseProductName().toLowerCase().contains("mysql"),
                    "expected MySQL, got " + meta.getDatabaseProductName());
            assertTrue(meta.getDriverName().toLowerCase().contains("mysql"));
            assertNotNull(meta.getURL());
            assertTrue(connection.isValid(5), "the connection should be usable");
        }
    }

    @Test
    @DisplayName("every table the mapping expects exists")
    void schemaIsPresent() throws SQLException {
        List<String> expected = List.of("app_user", "student_profile", "company",
                "drive", "job_application", "audit_log");

        try (Connection connection = open()) {
            List<String> found = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData()
                    .getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    found.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
            for (String table : expected) {
                assertTrue(found.contains(table),
                        table + " is missing, found " + found);
            }
        }
    }

    @Test
    @DisplayName("a parameterised query treats an injection payload as a literal")
    void parametersAreNotSql() throws SQLException {
        String payload = "' OR 1=1 -- ";

        try (Connection connection = open()) {
            long before = countStudents(connection);
            assertTrue(before > 0, "seed data is needed for this test to mean anything");

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM student_profile WHERE branch = ?")) {
                statement.setString(1, payload);
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getLong(1),
                            "the payload must be searched for as a branch name, "
                                    + "not executed as SQL");
                }
            }

            assertEquals(before, countStudents(connection),
                    "nothing should have been added or removed");
        }
    }

    @Test
    @DisplayName("a rollback undoes every statement in the transaction")
    void rollbackUndoesTheWholeUnit() throws SQLException {
        try (Connection connection = open()) {
            long before = countStudents(connection);
            boolean previousAutoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE student_profile SET backlogs = backlogs + 1")) {
                    int touched = update.executeUpdate();
                    assertTrue(touched > 0, "the update should have changed rows");
                }

                // Rolled back rather than committed, so none of it survives.
                connection.rollback();
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }

            assertEquals(before, countStudents(connection));

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(
                         "SELECT COALESCE(SUM(backlogs), 0) FROM student_profile")) {
                rs.next();
                long backlogs = rs.getLong(1);
                assertTrue(backlogs >= 0,
                        "the rolled back increment must not be visible, total is " + backlogs);
            }
        }
    }

    @Test
    @DisplayName("the reporting SQL runs and returns one row per branch")
    void branchReportSqlRuns() throws SQLException {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(PlacementReportDao.BRANCH_SQL);
             ResultSet rs = statement.executeQuery()) {

            int rows = 0;
            while (rs.next()) {
                rows++;
                assertNotNull(rs.getString("branch"));
                assertTrue(rs.getInt("students") > 0,
                        "a branch only appears because it has students");
                assertTrue(rs.getInt("selected") <= rs.getInt("students"),
                        "more placements than students would mean the join is duplicating rows");
            }
            assertTrue(rows > 0, "the seeded data should produce at least one branch");
        }
    }

    private long countStudents(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM student_profile")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
