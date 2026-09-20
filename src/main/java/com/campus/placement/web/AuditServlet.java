package com.campus.placement.web;

import com.campus.placement.ejb.AuditService;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The audit trail. Nothing on this page is written by a servlet: every row came
 * from the interceptor wrapped around a business method, or from the message
 * driven bean draining the notification queue.
 */
@WebServlet(name = "AuditServlet", urlPatterns = {"/admin/audit"})
public class AuditServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private AuditService auditService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String query = Validators.trim(request.getParameter("q"));
        int limit = Math.min(500, Math.max(20, Validators.toInt(request.getParameter("limit"), 100)));

        request.setAttribute("entries", query.isEmpty()
                ? auditService.recent(limit)
                : auditService.search(query, limit));
        request.setAttribute("query", query);
        request.setAttribute("limit", limit);
        request.setAttribute("total", auditService.count());

        Web.render(request, response, "admin-audit.jsp", "audit");
    }
}
