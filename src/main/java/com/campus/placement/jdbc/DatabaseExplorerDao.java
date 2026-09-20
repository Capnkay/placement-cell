package com.campus.placement.jdbc;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;

/**
 * Reads the database about itself, so the schema and its contents can be shown
 * inside the application instead of in a separate tool.
 *
 * <h2>The rules this class works under</h2>
 * <ul>
 *   <li>Every table name that reaches SQL is checked against the list the
 *       database itself reported. A name that is not on that list is refused,
 *       so an identifier can never be smuggled in. Identifiers cannot be bound
 *       as parameters, which is exactly why the whitelist has to exist.</li>
 *   <li>The console runs read only statements and nothing else. The check is a
 *       whitelist on the leading keyword plus a blocklist of anything that
 *       writes, and the connection is put into read only mode as well, so the
 *       server refuses a write even if the parser were fooled.</li>
 *   <li>Results are capped and the query is given a timeout, so a careless
 *       cross join cannot take the application down.</li>
 *   <li>Columns holding a password digest are never returned. There is no
 *       reason to read them and every reason not to.</li>
 * </ul>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DatabaseExplorerDao {

    /** Nothing beyond this many rows is fetched, whatever the query asks for. */
    public static final int MAX_ROWS = 200;

    /** A query that has not finished in this many seconds is cancelled. */
    public static final int TIMEOUT_SECONDS = 5;

    /** How many rows one page of the data browser shows. */
    public static final int PAGE_SIZE = 25;

    /** Column names whose contents are replaced before they leave this class. */
    private static final Set<String> MASKED_COLUMNS =
            Set.of("password_hash", "password", "secret", "token");

    /** A statement may begin with one of these and nothing else. */
    private static final Pattern READ_ONLY_START =
            Pattern.compile("^\\s*(select|with|show|describe|desc|explain)\\b",
                    Pattern.CASE_INSENSITIVE);

    /** Any of these appearing as a word is enough to refuse the statement. */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|create|truncate|rename|grant|revoke"
                    + "|replace|merge|call|do|handler|load|outfile|dumpfile|into|lock"
                    + "|unlock|prepare|execute|deallocate|set|start|commit|rollback"
                    + "|savepoint|shutdown|kill|use)\\b",
            Pattern.CASE_INSENSITIVE);

    @Resource(lookup = PlacementReportDao.JNDI_NAME)
    private DataSource dataSource;

    // ------------------------------------------------------------------
    // Schema
    // ------------------------------------------------------------------

    /**
     * The tables this application owns, as the database reports them, with a
     * live row count for each.
     */
    public Map<String, Long> listTables() throws SQLException {
        Map<String, Long> tables = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            for (String name : tableNames(connection)) {
                tables.put(name, countRows(connection, name));
            }
        }
        return tables;
    }

    /**
     * Full detail for one table: columns with their flags, the foreign keys that
     * make the relationships, and the indexes.
     */
    public TableDetail describe(String requestedTable) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String table = requireKnownTable(connection, requestedTable);

            TableDetail detail = new TableDetail();
            detail.setName(table);
            detail.setRowCount(countRows(connection, table));

            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();

            Set<String> primaryKeys = new LinkedHashSet<>();
            try (ResultSet rs = meta.getPrimaryKeys(catalog, null, table)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }

            try (ResultSet rs = meta.getColumns(catalog, null, table, "%")) {
                while (rs.next()) {
                    TableDetail.Column column = new TableDetail.Column();
                    String name = rs.getString("COLUMN_NAME");
                    column.setName(name);
                    column.setType(rs.getString("TYPE_NAME"));
                    column.setSize(rs.getInt("COLUMN_SIZE"));
                    column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getString("COLUMN_DEF"));
                    column.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                    column.setPrimaryKey(primaryKeys.contains(name));
                    column.setMasked(isMasked(name));
                    detail.getColumns().add(column);
                }
            }

            try (ResultSet rs = meta.getImportedKeys(catalog, null, table)) {
                while (rs.next()) {
                    TableDetail.ForeignKey key = new TableDetail.ForeignKey();
                    key.setName(rs.getString("FK_NAME"));
                    key.setColumn(rs.getString("FKCOLUMN_NAME"));
                    key.setReferencedTable(rs.getString("PKTABLE_NAME"));
                    key.setReferencedColumn(rs.getString("PKCOLUMN_NAME"));
                    detail.getForeignKeys().add(key);
                }
            }

            // An index spans one row of metadata per column, so they are gathered
            // by name and the column names joined back together.
            Map<String, TableDetail.Index> byName = new LinkedHashMap<>();
            try (ResultSet rs = meta.getIndexInfo(catalog, null, table, false, false)) {
                while (rs.next()) {
                    String name = rs.getString("INDEX_NAME");
                    String column = rs.getString("COLUMN_NAME");
                    if (name == null || column == null) {
                        continue;
                    }
                    TableDetail.Index index = byName.get(name);
                    if (index == null) {
                        index = new TableDetail.Index();
                        index.setName(name);
                        index.setUnique(!rs.getBoolean("NON_UNIQUE"));
                        index.setColumns(column);
                        byName.put(name, index);
                    } else {
                        index.setColumns(index.getColumns() + ", " + column);
                    }
                }
            }
            detail.getIndexes().addAll(byName.values());

            return detail;
        }
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /**
     * One page of rows from a table.
     *
     * <p>The table name is validated against the database's own list and then
     * quoted, because an identifier cannot be a bound parameter. The paging
     * numbers can be bound, and are.</p>
     */
    public ResultTable browse(String requestedTable, int page) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String table = requireKnownTable(connection, requestedTable);
            int offset = Math.max(0, page) * PAGE_SIZE;

            String sql = "SELECT * FROM `" + table + "` LIMIT ? OFFSET ?";
            long started = System.nanoTime();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(TIMEOUT_SECONDS);
                statement.setInt(1, PAGE_SIZE);
                statement.setInt(2, offset);

                try (ResultSet rs = statement.executeQuery()) {
                    ResultTable result = read(rs);
                    result.setSql(sql.replace("?", "…") + "   -- " + PAGE_SIZE + ", " + offset);
                    result.setElapsedMs((System.nanoTime() - started) / 1_000_000L);
                    return result;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // The console
    // ------------------------------------------------------------------

    /**
     * Runs one read only statement typed by the placement officer.
     *
     * <p>A refusal is returned as a result carrying an error rather than thrown,
     * because being told why a query was rejected is the useful behaviour here.</p>
     */
    public ResultTable runReadOnly(String submitted) {
        ResultTable result = new ResultTable();

        String sql = submitted == null ? "" : submitted.trim();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        result.setSql(sql);

        String refusal = whyRefused(sql);
        if (refusal != null) {
            result.setError(refusal);
            return result;
        }

        long started = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            // Defence in depth: even if the checks above were fooled, the server
            // itself now refuses to write on this connection.
            connection.setReadOnly(true);

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(TIMEOUT_SECONDS);
                statement.setMaxRows(MAX_ROWS);

                try (ResultSet rs = statement.executeQuery(sql)) {
                    ResultTable read = read(rs);
                    read.setSql(sql);
                    read.setElapsedMs((System.nanoTime() - started) / 1_000_000L);
                    read.setTruncated(read.getRowCount() >= MAX_ROWS);
                    return read;
                }
            } finally {
                connection.setReadOnly(false);
            }
        } catch (SQLException ex) {
            result.setError(ex.getMessage());
            result.setElapsedMs((System.nanoTime() - started) / 1_000_000L);
            return result;
        }
    }

    /**
     * @return null when the statement may run, otherwise the reason it may not
     */
    String whyRefused(String sql) {
        if (sql == null || sql.isBlank()) {
            return "Enter a statement to run.";
        }
        if (sql.contains(";")) {
            return "Only one statement at a time. A semicolon in the middle would "
                    + "let a second statement ride along behind the first.";
        }
        if (!READ_ONLY_START.matcher(sql).find()) {
            return "Only SELECT, WITH, SHOW, DESCRIBE and EXPLAIN are allowed here. "
                    + "This console reads, it does not write.";
        }
        if (FORBIDDEN.matcher(sql).find()) {
            return "That statement contains a keyword this console refuses. "
                    + "Anything that could write, lock or redirect output is blocked.";
        }
        if (sql.length() > 2000) {
            return "That statement is longer than two thousand characters.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Copies a result set into memory, masking any column that holds a secret
     * and shortening anything unreasonably long.
     */
    private ResultTable read(ResultSet rs) throws SQLException {
        ResultTable table = new ResultTable();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        boolean[] masked = new boolean[columnCount + 1];
        for (int i = 1; i <= columnCount; i++) {
            String label = meta.getColumnLabel(i);
            masked[i] = isMasked(label);
            table.addColumn(label, meta.getColumnTypeName(i));
        }

        while (rs.next()) {
            List<String> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                if (masked[i]) {
                    row.add("(hidden)");
                    continue;
                }
                String value = rs.getString(i);
                if (value == null) {
                    row.add(null);
                } else if (value.length() > 200) {
                    row.add(value.substring(0, 197) + "...");
                } else {
                    row.add(value);
                }
            }
            table.addRow(row);
        }
        return table;
    }

    private boolean isMasked(String columnName) {
        return columnName != null
                && MASKED_COLUMNS.contains(columnName.toLowerCase(Locale.ROOT));
    }

    /** Table names exactly as the database reports them. */
    private List<String> tableNames(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData()
                .getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME"));
            }
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    /**
     * The whitelist. A table name only becomes part of a statement after the
     * database has confirmed it exists, and the value used is the one the
     * database gave back rather than the one that arrived in the request.
     */
    private String requireKnownTable(Connection connection, String requested) throws SQLException {
        if (requested == null || requested.isBlank()) {
            throw new SQLException("No table was named");
        }
        for (String known : tableNames(connection)) {
            if (known.equalsIgnoreCase(requested.trim())) {
                return known;
            }
        }
        throw new SQLException("No such table in this database: " + requested);
    }

    private long countRows(Connection connection, String validatedTable) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM `" + validatedTable + "`")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
