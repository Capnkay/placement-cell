package com.campus.placement.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Parameter;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps every call to a bean that names it in {@code @Interceptors}.
 *
 * <p>The {@code @AroundInvoke} method runs before and after the real business
 * method, records who called what and how long it took, and hands control on
 * with {@code ctx.proceed()}. Nothing in the business beans knows this class
 * exists, which is the whole point of an interceptor: the audit requirement is
 * met without a single line of audit code in the logic being audited.</p>
 *
 * <p>Attached with {@code @Interceptors(AuditInterceptor.class)} on the bean
 * rather than through a CDI binding annotation. Both work; naming the class on
 * the bean is the more direct of the two to read, because the beans that are
 * audited say so themselves.</p>
 */
public class AuditInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class.getName());

    /**
     * The row is written through a bean rather than an entity manager held here,
     * because many audited methods are read only and run with no transaction.
     */
    @EJB
    private AuditWriter auditWriter;

    @Resource
    private SessionContext sessionContext;

    @AroundInvoke
    public Object record(InvocationContext ctx) throws Exception {
        long started = System.nanoTime();
        String method = ctx.getTarget().getClass().getSimpleName().replaceAll("\\$\\$.*", "")
                + "." + ctx.getMethod().getName();
        try {
            Object result = ctx.proceed();
            write(method, describe(ctx), millisSince(started), null);
            return result;
        } catch (Exception ex) {
            write(method, describe(ctx), millisSince(started), ex);
            throw ex;
        }
    }

    private long millisSince(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    /**
     * Renders the arguments, with one hard rule: anything that is a credential
     * is replaced by a placeholder. An audit trail that records the password
     * someone typed is worse than no audit trail at all, so the parameter names
     * are read from the class file (the build passes {@code -parameters}) and
     * any name that looks like a secret is never printed.
     */
    private String describe(InvocationContext ctx) {
        Object[] params = ctx.getParameters();
        if (params == null || params.length == 0) {
            return "no arguments";
        }
        Parameter[] declared = ctx.getMethod().getParameters();
        StringBuilder rendered = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                rendered.append(", ");
            }
            String name = i < declared.length ? declared[i].getName() : "arg" + i;
            if (isSecret(name)) {
                rendered.append("[redacted]");
            } else if (params[i] == null) {
                rendered.append("null");
            } else {
                rendered.append(shorten(String.valueOf(params[i])));
            }
        }
        return shorten(rendered.toString());
    }

    private boolean isSecret(String parameterName) {
        String lower = parameterName.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("secret")
                || lower.contains("token") || lower.contains("credential");
    }

    private String shorten(String value) {
        return value.length() > 160 ? value.substring(0, 157) + "..." : value;
    }

    private void write(String action, String detail, long millis, Exception failure) {
        String actor = "system";
        try {
            if (sessionContext != null && sessionContext.getCallerPrincipal() != null) {
                String principal = sessionContext.getCallerPrincipal().getName();
                // The container hands back ANONYMOUS when no container managed
                // realm is in play, which is the case here: sign in is done by
                // the application's own filter rather than by a server realm.
                actor = "ANONYMOUS".equalsIgnoreCase(principal) ? "portal" : principal;
            }
        } catch (RuntimeException ignored) {
            // No caller principal outside a security context, "system" is correct.
        }
        String suffix = failure == null ? "" : " [failed: " + failure.getClass().getSimpleName() + "]";
        try {
            auditWriter.write(actor, action, detail + suffix, millis);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Audit row could not be written for " + action, ex);
        }
    }
}
