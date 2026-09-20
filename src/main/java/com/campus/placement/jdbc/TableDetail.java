package com.campus.placement.jdbc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything {@link java.sql.DatabaseMetaData} reports about one table.
 *
 * <p>None of this is hard coded. The columns, the keys and the indexes are all
 * read back from the live database, so what the page shows is what MySQL
 * actually built from the JPA mapping, not what the mapping intended.</p>
 */
public class TableDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private long rowCount;
    private final List<Column> columns = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private final List<Index> indexes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long rowCount) {
        this.rowCount = rowCount;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    /** One column, with the flags that decide what may be stored in it. */
    public static class Column implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String type;
        private int size;
        private boolean nullable;
        private boolean primaryKey;
        private boolean autoIncrement;
        private String defaultValue;
        /** True for columns whose contents are never shown, such as the digest. */
        private boolean masked;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public boolean isAutoIncrement() {
            return autoIncrement;
        }

        public void setAutoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public boolean isMasked() {
            return masked;
        }

        public void setMasked(boolean masked) {
            this.masked = masked;
        }
    }

    /** A foreign key, which is the relationship the ORM mapped. */
    public static class ForeignKey implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String column;
        private String referencedTable;
        private String referencedColumn;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public String getReferencedTable() {
            return referencedTable;
        }

        public void setReferencedTable(String referencedTable) {
            this.referencedTable = referencedTable;
        }

        public String getReferencedColumn() {
            return referencedColumn;
        }

        public void setReferencedColumn(String referencedColumn) {
            this.referencedColumn = referencedColumn;
        }
    }

    /** An index, unique or otherwise. */
    public static class Index implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;
        private String columns;
        private boolean unique;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColumns() {
            return columns;
        }

        public void setColumns(String columns) {
            this.columns = columns;
        }

        public boolean isUnique() {
            return unique;
        }

        public void setUnique(boolean unique) {
            this.unique = unique;
        }
    }
}
