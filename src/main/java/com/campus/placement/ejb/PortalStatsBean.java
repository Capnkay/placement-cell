package com.campus.placement.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Singleton session bean: exactly one instance for the whole application.
 *
 * <p>It holds the counters that the footer shows, and it is also where the JNDI
 * lookup is demonstrated. The datasource is reached two ways on purpose: by
 * injection with {@code @Resource}, which is what production code does, and by
 * an explicit {@code InitialContext} lookup, which is what the naming service
 * is doing underneath.</p>
 *
 * <p>Container managed concurrency is on, so a WRITE lock serialises the
 * increments and a READ lock lets the dashboards read in parallel.</p>
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class PortalStatsBean {

    private static final Logger LOG = Logger.getLogger(PortalStatsBean.class.getName());

    /**
     * Injected by the container from the application scoped JNDI namespace. This
     * is the same pool Hibernate and the reporting DAO use, not the server's
     * built in default datasource.
     */
    @Resource(lookup = "java:app/jdbc/placementDS")
    private DataSource dataSource;

    @Resource
    private ManagedExecutorService executor;

    private LocalDateTime startedAt;
    private long logins;
    private long failedLogins;
    private long applicationsFiled;
    private String datasourceProduct = "unknown";
    private final Map<String, String> jndiReport = new LinkedHashMap<>();

    @PostConstruct
    public void warmUp() {
        startedAt = LocalDateTime.now();
        readDatabaseBanner();
        runJndiLookups();
        LOG.info("Placement portal singleton is up, backing database is " + datasourceProduct);
    }

    /**
     * The basic lookup: ask the naming service for a name and get back a
     * resource reference. This is the call {@code @Resource} saves you from
     * writing by hand.
     */
    private void runJndiLookups() {
        String[] names = {
                "java:app/jdbc/placementDS",
                "java:app/jms/NotificationQueue",
                "java:app/jms/NotificationFactory",
                "java:comp/env",
                "java:module/ModuleName",
                "java:app/AppName"
        };
        try {
            Context context = new InitialContext();
            for (String name : names) {
                try {
                    Object bound = context.lookup(name);
                    jndiReport.put(name, bound == null ? "bound to null"
                            : bound.getClass().getName());
                } catch (NamingException ex) {
                    jndiReport.put(name, "not bound: " + ex.getClass().getSimpleName());
                }
            }
        } catch (NamingException ex) {
            LOG.warning("The initial naming context is unavailable: " + ex.getMessage());
        }
    }

    private void readDatabaseBanner() {
        if (dataSource == null) {
            return;
        }
        try (var connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            datasourceProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (Exception ex) {
            datasourceProduct = "unavailable: " + ex.getMessage();
        }
    }

    @Lock(LockType.WRITE)
    public void recordLogin() {
        logins++;
    }

    @Lock(LockType.WRITE)
    public void recordFailedLogin() {
        failedLogins++;
    }

    @Lock(LockType.WRITE)
    public void recordApplication() {
        applicationsFiled++;
    }

    @Lock(LockType.READ)
    public long getLogins() {
        return logins;
    }

    @Lock(LockType.READ)
    public long getFailedLogins() {
        return failedLogins;
    }

    @Lock(LockType.READ)
    public long getApplicationsFiled() {
        return applicationsFiled;
    }

    @Lock(LockType.READ)
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    @Lock(LockType.READ)
    public String getDatasourceProduct() {
        return datasourceProduct;
    }

    @Lock(LockType.READ)
    public Map<String, String> getJndiReport() {
        return new LinkedHashMap<>(jndiReport);
    }

    @Lock(LockType.READ)
    public boolean isExecutorAvailable() {
        return executor != null;
    }
}
