package com.campus.placement.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sets the response headers that tell the browser what this application is
 * allowed to do. Applied to every request, including the sign in page and the
 * error pages, because those are exactly the pages an attacker would rather the
 * rules did not cover.
 *
 * <h2>What each header is for</h2>
 * <ul>
 *   <li><b>Content-Security-Policy</b> is the important one.
 *       {@code script-src 'self'} with no {@code 'unsafe-inline'} means the
 *       browser refuses to run any script that is not served from this
 *       application's own origin. If a stored cross site scripting hole did
 *       exist somewhere, the injected {@code <script>} would sit in the page and
 *       never execute. This is only possible because no page here carries an
 *       inline script or an onclick attribute: all of it lives in app.js.</li>
 *   <li>{@code style-src} does allow inline styles. A handful of one off layout
 *       values are written as style attributes, and an injected style can at
 *       worst deface a page, not run code. The trade is deliberate and recorded
 *       in SECURITY.md rather than quietly made.</li>
 *   <li>{@code frame-ancestors 'none'} and <b>X-Frame-Options</b> stop the
 *       portal being loaded inside an invisible frame on another site and
 *       clicked through, which is clickjacking.</li>
 *   <li>{@code form-action 'self'} means a form on this site cannot be made to
 *       post somewhere else, so an injected form cannot harvest a password.</li>
 *   <li><b>X-Content-Type-Options: nosniff</b> stops the browser second guessing
 *       a declared content type, which is what turns an uploaded file into a
 *       script when it is served back.</li>
 *   <li><b>Referrer-Policy</b> keeps the path of the page a student came from
 *       out of requests to other sites.</li>
 * </ul>
 *
 * <p>Strict-Transport-Security is deliberately not set. It would be wrong here:
 * the project runs over plain HTTP on localhost, and sending it would tell the
 * browser to refuse HTTP to this host for months afterwards.</p>
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class SecurityHeadersFilter implements Filter {

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "object-src 'none'");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Anything that is not a static asset holds data belonging to one person,
        // so it must not sit in the browser cache where the next user of a shared
        // machine can press Back and read it.
        String path = request.getRequestURI();
        if (!path.contains("/assets/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        chain.doFilter(req, res);
    }
}
