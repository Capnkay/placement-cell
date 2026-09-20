package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.DriveService;
import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.JobApplication;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * The application pipeline as the placement officer works it: filter by drive
 * or status, then move a candidate along with a remark that the student sees.
 */
@WebServlet(name = "AdminApplicationServlet", urlPatterns = {"/admin/applications"})
public class AdminApplicationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private ApplicationService applicationService;

    @EJB
    private DriveService driveService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long driveId = Validators.toLong(request.getParameter("drive"));
        String statusFilter = Validators.trim(request.getParameter("status"));

        List<JobApplication> applications = driveId == null
                ? applicationService.listAll()
                : applicationService.forDrive(driveId);

        if (!statusFilter.isEmpty()) {
            try {
                ApplicationStatus wanted = ApplicationStatus.valueOf(statusFilter);
                applications = applications.stream()
                        .filter(a -> a.getStatus() == wanted)
                        .toList();
            } catch (IllegalArgumentException ex) {
                statusFilter = "";
            }
        }

        request.setAttribute("applications", applications);
        request.setAttribute("drives", driveService.listAll());
        request.setAttribute("statuses", ApplicationStatus.values());
        request.setAttribute("selectedDrive", driveId);
        request.setAttribute("selectedStatus", statusFilter);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "admin-applications.jsp", "applications");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        Long applicationId = Validators.toLong(request.getParameter("applicationId"));
        String statusRaw = Validators.trim(request.getParameter("status"));
        String remarks = Validators.trim(request.getParameter("remarks"));
        String back = Validators.trim(request.getParameter("back"));

        JobApplication application = applicationService.find(applicationId);
        if (application == null) {
            Web.flashError(request, "That application could not be found.");
            Web.redirect(request, response, "/admin/applications");
            return;
        }

        ApplicationStatus status;
        try {
            status = ApplicationStatus.valueOf(statusRaw);
        } catch (IllegalArgumentException ex) {
            Web.flashError(request, "That is not a valid status.");
            Web.redirect(request, response, "/admin/applications");
            return;
        }

        if (remarks.length() > 500) {
            remarks = remarks.substring(0, 500);
        }

        applicationService.updateStatus(applicationId, status,
                remarks.isEmpty() ? null : remarks);

        Drive drive = application.getDrive();
        Web.flashSuccess(request, application.getStudent().getUser().getFullName()
                + " is now marked " + status.getLabel() + " for "
                + drive.getCompany().getName() + ".");

        Web.redirect(request, response, back.startsWith("/admin/")
                ? back : "/admin/applications");
    }
}
