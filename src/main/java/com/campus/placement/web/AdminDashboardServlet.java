package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.AuditService;
import com.campus.placement.ejb.DriveService;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.JobApplication;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The placement officer's landing page: the numbers that matter, the pipeline
 * across all drives, and the most recent activity.
 */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StudentService studentService;

    @EJB
    private DriveService driveService;

    @EJB
    private ApplicationService applicationService;

    @EJB
    private AuditService auditService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Map<ApplicationStatus, Long> breakdown = applicationService.statusBreakdown();
        List<JobApplication> all = applicationService.listAll();
        long students = studentService.countStudents();
        long placed = applicationService.countPlaced();

        request.setAttribute("studentCount", students);
        request.setAttribute("driveCount", driveService.listAll().size());
        request.setAttribute("openDriveCount", driveService.countOpen());
        request.setAttribute("companyCount", driveService.listCompanies().size());
        request.setAttribute("applicationCount", applicationService.countAll());
        request.setAttribute("placedCount", placed);
        request.setAttribute("placementRate", students == 0 ? 0
                : Math.round(placed * 1000.0 / students) / 10.0);
        request.setAttribute("breakdown", breakdown);
        request.setAttribute("recent", all.size() > 8 ? all.subList(0, 8) : all);
        request.setAttribute("audit", auditService.recent(6));

        long peak = breakdown.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        request.setAttribute("peak", Math.max(1L, peak));

        Web.render(request, response, "admin-dashboard.jsp", "dashboard");
    }
}
