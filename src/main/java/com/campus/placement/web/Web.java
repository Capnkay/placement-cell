package com.campus.placement.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shared names and small helpers for the controller layer. Keeping the session
 * and request attribute keys in one place stops a typo in a JSP from silently
 * rendering an empty page.
 */
public final class Web {

    public static final String SESSION_USER = "user";
    public static final String SESSION_CSRF = "csrfToken";
    public static final String SESSION_SHORTLIST = "shortlistBean";
    public static final String SESSION_WELCOME_NAME = "welcomeName";
    public static final String SESSION_WELCOME_EMAIL = "welcomeEmail";
    public static final String SESSION_WELCOME_ROLL = "welcomeRoll";
    public static final String FLASH_SUCCESS = "flashSuccess";
    public static final String FLASH_ERROR = "flashError";
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_PAGE = "activePage";

    public static final String COOKIE_REMEMBER_EMAIL = "remember_email";
    public static final String COOKIE_THEME = "portal_theme";
    public static final String COOKIE_LAST_VISIT = "last_visit";

    /** Views live under WEB-INF so no one can request a JSP directly. */
    private static final String VIEW_ROOT = "/WEB-INF/views/";

    private static final SecureRandom RANDOM = new SecureRandom();

    private Web() {
    }

    /**
     * Forwards through the RequestDispatcher. Control stays on the server and
     * the browser address bar keeps the servlet URL, which is what makes the
     * JSPs unreachable from outside.
     */
    public static void render(HttpServletRequest request, HttpServletResponse response,
                              String view, String activePage)
            throws ServletException, IOException {
        request.setAttribute(ATTR_PAGE, activePage);
        request.getRequestDispatcher(VIEW_ROOT + view).forward(request, response);
    }

    public static SessionUser current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(SESSION_USER);
    }

    /** Post then redirect then get, so a refresh never repeats a write. */
    public static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    public static void flashSuccess(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_SUCCESS, message);
    }

    public static void flashError(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_ERROR, message);
    }

    // Cross site request forgery protection.

    /** Returns the token for this session, creating it on first use. */
    public static String csrfToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_CSRF);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_CSRF, token);
        }
        return token;
    }

    /**
     * Every state changing POST carries the session token in a hidden field.
     * A form submitted from another site cannot read that value, so the missing
     * or wrong token is what gives the forgery away.
     */
    public static boolean csrfValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String expected = (String) session.getAttribute(SESSION_CSRF);
        String supplied = request.getParameter("csrf");
        if (expected == null || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Escapes a value that is about to be written into a header or a log line. */
    public static String safeHeaderValue(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\"]", "");
    }
}
