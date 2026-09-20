package com.campus.placement.jdbc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link java.sql.ResultSet} read into memory so a JSP can render it.
 *
 * <p>The result set itself cannot be handed to a view: it is tied to an open
 * connection, and the connection is closed the moment the DAO method returns.
 * Passing it out and iterating it in a page is the classic way to leak a
 * connection until the pool runs dry. So the rows are copied here first and the
 * connection is released immediately.</p>
 */
public class ResultTable implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<String> columns = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    private boolean truncated;
    private long elapsedMs;
    private String sql;
    private String error;

    public void addColumn(String name, String type) {
        columns.add(name);
        types.add(type);
    }

    public void addRow(List<String> row) {
        rows.add(row);
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** True when the row cap was reached and there may be more rows behind it. */
    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isFailed() {
        return error != null;
    }
}
