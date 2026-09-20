package com.campus.placement.web;

import com.campus.placement.ejb.ShortlistBean;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Ends the session properly: any open stateful bean conversation is closed with
 * its {@code @Remove} method so the container does not have to wait for the
 * timeout, then the session itself is invalidated. Every attribute goes with
 * it, which is why signing out cannot leave a stale role behind.
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout"})
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object bean = session.getAttribute(Web.SESSION_SHORTLIST);
            if (bean instanceof ShortlistBean shortlist) {
                try {
                    shortlist.finish();
                } catch (RuntimeException ignored) {
                    // The bean may already have timed out, nothing to clean up.
                }
            }
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login?reason="
                + java.net.URLEncoder.encode("You have been signed out.",
                java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
