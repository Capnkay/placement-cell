package com.campus.placement.web;

import com.campus.placement.ejb.PortalStatsBean;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A window onto the container itself: which server is running the application,
 * what the naming service has bound, how many sessions are alive, and the
 * counters held by the singleton bean.
 */
@WebServlet(name = "SystemServlet", urlPatterns = {"/admin/system"})
public class SystemServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private PortalStatsBean stats;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Map<String, String> container = new LinkedHashMap<>();
        container.put("Server", getServletContext().getServerInfo());
        container.put("Servlet API", getServletContext().getMajorVersion()
                + "." + getServletContext().getMinorVersion());
        container.put("Context path", getServletContext().getContextPath().isEmpty()
                ? "/" : getServletContext().getContextPath());
        container.put("Java runtime", System.getProperty("java.vendor")
                + " " + System.getProperty("java.version"));
        container.put("Operating system", System.getProperty("os.name")
                + " " + System.getProperty("os.version"));
        container.put("Database", stats.getDatasourceProduct());
        container.put("Managed executor", stats.isExecutorAvailable() ? "available" : "not available");

        Map<String, String> counters = new LinkedHashMap<>();
        counters.put("Successful sign ins", String.valueOf(stats.getLogins()));
        counters.put("Rejected sign ins", String.valueOf(stats.getFailedLogins()));
        counters.put("Applications filed this run", String.valueOf(stats.getApplicationsFiled()));
        counters.put("Sessions open now", String.valueOf(SessionTracker.getActive()));
        counters.put("Sessions created since start", String.valueOf(SessionTracker.getTotal()));

        request.setAttribute("container", container);
        request.setAttribute("counters", counters);
        request.setAttribute("jndi", stats.getJndiReport());
        request.setAttribute("startedAt", stats.getStartedAt());

        Web.render(request, response, "admin-system.jsp", "system");
    }
}
