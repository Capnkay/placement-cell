package com.campus.placement.security;

/**
 * Turns a raw request URI into the shape the container routes on, so an access
 * rule is applied to the same path the servlet mapping sees.
 *
 * <p>Kept free of servlet types so the rule can be tested on its own.</p>
 */
public final class RequestPaths {

    private RequestPaths() {
    }

    /**
     * The path with the context path and any path parameter removed and repeated
     * slashes collapsed.
     *
     * <p>The container matches {@code //admin/database} to the same servlet as
     * {@code /admin/database}, but the raw request URI still contains both
     * slashes. Testing the raw text with {@code startsWith("/admin/")} therefore
     * let a student straight past the officer check.</p>
     */
    public static String canonical(String requestUri, String contextPath) {
        String path = requestUri.substring(contextPath.length());
        int parameters = path.indexOf(';');
        if (parameters >= 0) {
            path = path.substring(0, parameters);
        }
        return path.replaceAll("/{2,}", "/");
    }

    /** True for the officer area, which is everything under {@code /admin}. */
    public static boolean isAdminPath(String path) {
        return path.equals("/admin") || path.startsWith("/admin/");
    }
}
