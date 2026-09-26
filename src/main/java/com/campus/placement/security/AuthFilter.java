package com.campus.placement.security;

import com.campus.placement.web.SessionUser;
import com.campus.placement.web.Web;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The single gate in front of every signed in page.
 *
 * <p>Four things happen here, in order:</p>
 * <ol>
 *   <li>no session or no user in it means the request never reaches a servlet,
 *       it is bounced to the login page with the original URL remembered</li>
 *   <li>anything under {@code /admin/} additionally requires the officer role,
 *       so a student who types an admin URL by hand is refused</li>
 *   <li>every state changing POST must carry the session CSRF token</li>
 *   <li>protected responses are marked no-store, so pressing Back after signing
 *       out shows the login page rather than a cached dashboard</li>
 * </ol>
 */
// asyncSupported has to be declared here as well as on the servlet. A filter that
// does not support async makes startAsync() illegal for everything behind it, which
// would break the non blocking import even though that servlet declares it.
@WebFilter(urlPatterns = {"/app/*", "/admin/*"}, asyncSupported = true)
public class AuthFilter implements Filter {

    private static final String LOGIN = "/login";

    @Override
    public void init(FilterConfig config) {
        // Nothing to configure, the rules are static.
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = RequestPaths.canonical(request.getRequestURI(), request.getContextPath());

        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(Web.SESSION_USER);

        if (user == null) {
            bounceToLogin(request, response, path, "Please sign in to continue.");
            return;
        }

        if (RequestPaths.isAdminPath(path) && !user.isAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.setAttribute("deniedPath", path);
            request.getRequestDispatcher("/WEB-INF/views/error-403.jsp").forward(request, response);
            return;
        }

        boolean tokenOk;
        try {
            tokenOk = !"POST".equalsIgnoreCase(request.getMethod()) || Web.csrfValid(request);
        } catch (RuntimeException tooLarge) {
            // Reading the token from a multipart body makes the container parse
            // the whole upload, and it refuses one over the configured limit.
            // That is a user's oversize file, not a server fault, so say so.
            Web.flashError(request, "That file is larger than the two megabyte limit.");
            response.sendRedirect(request.getContextPath()
                    + (user.isAdmin() ? "/admin/dashboard" : "/app/profile"));
            return;
        }
        if (!tokenOk) {
            Web.flashError(request,
                    "That form could not be verified, most likely because it sat open too long. "
                            + "Please try the action again.");
            response.sendRedirect(request.getContextPath()
                    + (user.isAdmin() ? "/admin/dashboard" : "/app/dashboard"));
            return;
        }

        // Caching and the other protective headers are set for every request by
        // SecurityHeadersFilter, including the sign in and error pages. This
        // filter is left to do only the thing it alone can do, which is decide
        // whether this person may see this page at all.
        chain.doFilter(request, response);
    }

    private void bounceToLogin(HttpServletRequest request, HttpServletResponse response,
                               String path, String message) throws IOException {
        String target = path;
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            target = target + "?" + query;
        }
        String redirect = request.getContextPath() + LOGIN
                + "?reason=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                + "&next=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        response.sendRedirect(redirect);
    }

    @Override
    public void destroy() {
        // Nothing held open.
    }
}
