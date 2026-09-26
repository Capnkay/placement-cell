package com.campus.placement.web;

import com.campus.placement.ejb.AuthResult;
import com.campus.placement.ejb.AuthService;
import com.campus.placement.ejb.PortalStatsBean;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.AppUser;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Front door of the portal.
 *
 * <p>The order of checks matters and is worth reading: the form token is
 * verified first, then the email is rejected outright if it is not a well
 * formed address, and only then is the database asked anything. An address that
 * fails the pattern never reaches a query.</p>
 *
 * <p>On success the session id is regenerated with {@code changeSessionId()}.
 * Without that step an attacker who fixed a session id before sign in would
 * still hold a valid one afterwards, which is the session fixation attack.</p>
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int REMEMBER_DAYS = 30;

    @EJB
    private AuthService authService;

    @EJB
    private StudentService studentService;

    @EJB
    private PortalStatsBean stats;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser existing = Web.current(request);
        if (existing != null) {
            Web.redirect(request, response, existing.isAdmin() ? "/admin/dashboard" : "/app/dashboard");
            return;
        }

        HttpSession session = request.getSession(true);
        request.setAttribute("csrf", Web.csrfToken(session));
        request.setAttribute("reason", Validators.trim(request.getParameter("reason")));
        request.setAttribute("next", safeNext(request.getParameter("next")));
        request.setAttribute("rememberedEmail", readCookie(request, Web.COOKIE_REMEMBER_EMAIL));

        // Someone who has just registered arrives here once. Show them a welcome,
        // fill in their email, and put the cursor where the next keystroke belongs.
        String welcomeEmail = (String) session.getAttribute(Web.SESSION_WELCOME_EMAIL);
        if (welcomeEmail != null) {
            String fullName = (String) session.getAttribute(Web.SESSION_WELCOME_NAME);
            request.setAttribute("welcomeName", fullName == null ? "" : firstName(fullName));
            request.setAttribute("welcomeEmail", welcomeEmail);
            request.setAttribute("welcomeRoll", session.getAttribute(Web.SESSION_WELCOME_ROLL));
            request.setAttribute("email", welcomeEmail);
            session.removeAttribute(Web.SESSION_WELCOME_NAME);
            session.removeAttribute(Web.SESSION_WELCOME_EMAIL);
            session.removeAttribute(Web.SESSION_WELCOME_ROLL);
        }
        request.setAttribute("lastVisit", readCookie(request, Web.COOKIE_LAST_VISIT));

        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = Validators.trim(request.getParameter("email"));
        String password = request.getParameter("password");
        boolean remember = "on".equals(request.getParameter("remember"));
        String next = safeNext(request.getParameter("next"));

        HttpSession session = request.getSession(true);

        if (!Web.csrfValid(request)) {
            fail(request, response, email, next,
                    "This page was open for too long and could not be verified. Please sign in again.");
            return;
        }

        AuthResult result = authService.authenticate(email, password);

        if (!result.isSuccess()) {
            stats.recordFailedLogin();
            fail(request, response, email, next, result.getMessage());
            return;
        }

        AppUser user = result.getUser();

        // Session fixation defence: throw away the pre login id, keep the attributes.
        request.changeSessionId();

        Long profileId = studentService.findByUser(user.getId())
                .map(p -> p.getId())
                .orElse(null);
        SessionUser sessionUser = new SessionUser(user, profileId);
        session.setAttribute(Web.SESSION_USER, sessionUser);
        Web.csrfToken(session);
        stats.recordLogin();

        // No last visit cookie means this browser has never signed in here, so
        // "welcome back" would be wrong. Read it before the cookie is rewritten.
        boolean firstVisit = readCookie(request, Web.COOKIE_LAST_VISIT) == null
                || readCookie(request, Web.COOKIE_LAST_VISIT).isEmpty();
        writeCookie(request, response, Web.COOKIE_REMEMBER_EMAIL,
                remember ? user.getEmail() : null, REMEMBER_DAYS);
        writeCookie(request, response, Web.COOKIE_LAST_VISIT,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                REMEMBER_DAYS);

        Web.flashSuccess(request, (firstVisit ? "Welcome to the placement cell, " : "Welcome back, ")
                + user.getFullName() + ".");

        if (!next.isEmpty() && (!next.startsWith("/admin/") || sessionUser.isAdmin())) {
            Web.redirect(request, response, next);
            return;
        }
        Web.redirect(request, response, sessionUser.isAdmin() ? "/admin/dashboard" : "/app/dashboard");
    }

    private void fail(HttpServletRequest request, HttpServletResponse response,
                      String email, String next, String message)
            throws ServletException, IOException {
        request.setAttribute(Web.ATTR_ERROR, message);
        request.setAttribute("email", email);
        request.setAttribute("next", next);
        request.setAttribute("csrf", Web.csrfToken(request.getSession(true)));
        request.setAttribute("rememberedEmail", readCookie(request, Web.COOKIE_REMEMBER_EMAIL));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    /**
     * Only an in application path is ever followed after sign in. Anything with
     * a scheme, a host or a protocol relative prefix is dropped, so the login
     * form cannot be turned into an open redirect.
     */
    /** "Demo Student" becomes "Demo", which is how a welcome should sound. */
    private String firstName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private String safeNext(String candidate) {
        String value = Validators.trim(candidate);
        if (value.isEmpty()
                || !value.startsWith("/")
                || value.startsWith("//")
                || value.contains(":")
                || value.contains("\\")) {
            return "";
        }
        return value;
    }

    static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String raw = cookie.getValue();
                if (raw == null || raw.isEmpty()) {
                    return "";
                }
                // Values are stored percent encoded so a space or a comma cannot
                // break the Set-Cookie header.
                return java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static void writeCookie(HttpServletRequest request, HttpServletResponse response,
                            String name, String value, int days) {
        String encoded = value == null ? ""
                : java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        Cookie cookie = new Cookie(name, encoded);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(value == null ? 0 : (int) java.time.Duration.ofDays(days).getSeconds());
        response.addCookie(cookie);
    }
}
