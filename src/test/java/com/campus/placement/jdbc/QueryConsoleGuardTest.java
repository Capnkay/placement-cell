package com.campus.placement.jdbc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The query console is the one place in the application that runs SQL somebody
 * typed. These tests pin what it accepts and what it refuses.
 *
 * <p>The guard is only the first of three layers. The connection is also put
 * into read only mode, and the database user is granted rights on one schema and
 * nothing else. This class tests the first layer, which is the one that would be
 * easiest to weaken by accident later.</p>
 */
@DisplayName("Query console guard")
class QueryConsoleGuardTest {

    private final DatabaseExplorerDao dao = new DatabaseExplorerDao();

    @ParameterizedTest
    @DisplayName("read only statements are allowed")
    @ValueSource(strings = {
            "SELECT * FROM student_profile",
            "select branch, count(*) from student_profile group by branch",
            "  SELECT 1  ",
            "SELECT * FROM drive WHERE package_lpa > 8 ORDER BY package_lpa DESC",
            "WITH top AS (SELECT * FROM drive) SELECT * FROM top",
            "SHOW TABLES",
            "DESCRIBE app_user",
            "EXPLAIN SELECT * FROM job_application",
            "SELECT * FROM app_user;"
    })
    void allowsReads(String sql) {
        assertNull(dao.whyRefused(sql.trim().replaceAll(";+$", "")),
                sql + " should have been allowed");
    }

    @ParameterizedTest
    @DisplayName("anything that writes is refused")
    @ValueSource(strings = {
            "DROP TABLE student_profile",
            "delete from app_user",
            "UPDATE student_profile SET cgpa = 10",
            "INSERT INTO company (name) VALUES ('x')",
            "TRUNCATE audit_log",
            "ALTER TABLE drive ADD COLUMN x INT",
            "CREATE TABLE evil (id INT)",
            "GRANT ALL ON *.* TO 'x'@'localhost'",
            "RENAME TABLE drive TO drive2"
    })
    void refusesWrites(String sql) {
        assertNotNull(dao.whyRefused(sql), sql + " should have been refused");
    }

    @Test
    @DisplayName("a second statement cannot ride along behind the first")
    void refusesStackedStatements() {
        String refusal = dao.whyRefused("SELECT 1; DROP TABLE drive");
        assertNotNull(refusal);
        // The semicolon check has to fire before the leading keyword check, or a
        // statement starting with SELECT would carry anything after it.
        assertNotNull(dao.whyRefused("SELECT * FROM app_user; DELETE FROM app_user"));
    }

    @Test
    @DisplayName("a read that writes to a file is refused")
    void refusesOutfile() {
        assertNotNull(dao.whyRefused("SELECT * FROM app_user INTO OUTFILE '/tmp/dump'"));
        assertNotNull(dao.whyRefused("SELECT * FROM app_user INTO DUMPFILE '/tmp/dump'"));
    }

    @Test
    @DisplayName("session and transaction control are refused")
    void refusesSessionControl() {
        assertNotNull(dao.whyRefused("SET GLOBAL general_log = 'ON'"));
        assertNotNull(dao.whyRefused("START TRANSACTION"));
        assertNotNull(dao.whyRefused("USE mysql"));
        assertNotNull(dao.whyRefused("LOCK TABLES drive WRITE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("an empty statement is refused rather than run")
    void refusesEmpty(String sql) {
        assertNotNull(dao.whyRefused(sql));
    }

    @Test
    @DisplayName("an absurdly long statement is refused")
    void refusesOverLongStatements() {
        String padded = "SELECT * FROM app_user WHERE email IN ("
                + "'a',".repeat(600) + "'b')";
        assertNotNull(dao.whyRefused(padded));
    }

    @Test
    @DisplayName("the refusal explains itself")
    void refusalsAreReadable() {
        assertNotNull(dao.whyRefused("DROP TABLE drive"));
        String message = dao.whyRefused("DROP TABLE drive");
        // A refusal that does not say why is a refusal somebody will work around.
        assertNotNull(message);
        org.junit.jupiter.api.Assertions.assertTrue(message.length() > 20,
                "the reason should be a sentence, not a code");
    }
}
