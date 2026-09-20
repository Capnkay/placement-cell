package com.campus.placement.jdbc;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What {@code DatabaseMetaData} reports about the live connection.
 *
 * <p>Shown on the reports page so the connection can be pointed at rather than
 * described: which driver is loaded, which server answered, and which JNDI name
 * the pool was reached through.</p>
 */
public class ConnectionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> values = new LinkedHashMap<>();
    private boolean available;
    private String failure;

    public void put(String key, String value) {
        values.put(key, value);
    }

    public Map<String, String> getValues() {
        return values;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getFailure() {
        return failure;
    }

    public void setFailure(String failure) {
        this.failure = failure;
    }
}
