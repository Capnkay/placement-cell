package com.campus.placement.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Cookie driven appearance switch.
 *
 * <p>The chosen palette is kept in a persistent cookie rather than the session,
 * which is the difference worth noticing: the session ends when the browser
 * closes or the timeout fires, while this cookie survives both and still
 * applies the next morning. Nothing secret is stored in it, which is the rule
 * for anything that lives on the client.</p>
 */
@WebServlet(name = "ThemeServlet", urlPatterns = {"/theme"})
public class ThemeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final int ONE_YEAR = 365 * 24 * 60 * 60;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requested = request.getParameter("mode");
        String mode = "dark".equals(requested) ? "dark" : "light";

        Cookie cookie = new Cookie(Web.COOKIE_THEME, mode);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setMaxAge(ONE_YEAR);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);

        String back = request.getParameter("back");
        if (back == null || back.isBlank() || !back.startsWith("/")
                || back.startsWith("//") || back.contains(":")) {
            back = "/app/dashboard";
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
