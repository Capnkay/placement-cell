package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.DriveService;
import com.campus.placement.ejb.Eligibility;
import com.campus.placement.ejb.PortalStatsBean;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.JobApplication;
import com.campus.placement.entity.StudentProfile;
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
 * Applying to a drive and reviewing what has been applied to.
 *
 * <p>The eligibility rules are re-run inside the POST even though the board
 * already hid the button. A page can be stale, a form can be replayed, and a
 * request can be hand written, so the check that matters is the one on the
 * write path.</p>
 */
@WebServlet(name = "ApplicationServlet", urlPatterns = {"/app/applications", "/app/apply", "/app/withdraw"})
public class ApplicationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private ApplicationService applicationService;

    @EJB
    private DriveService driveService;

    @EJB
    private StudentService studentService;

    @EJB
    private PortalStatsBean stats;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.redirect(request, response, "/logout");
            return;
        }

        List<JobApplication> applications = applicationService.forStudent(profile.getId());
        request.setAttribute("profile", profile);
        request.setAttribute("applications", applications);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "my-applications.jsp", "applications");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.redirect(request, response, "/logout");
            return;
        }

        if ("/app/withdraw".equals(request.getServletPath())) {
            withdraw(request, response, profile);
            return;
        }

        Long driveId = Validators.toLong(request.getParameter("driveId"));
        Drive drive = driveService.find(driveId);
        if (drive == null) {
            Web.flashError(request, "That drive could not be found.");
            Web.redirect(request, response, "/app/drives");
            return;
        }

        Eligibility verdict = driveService.check(profile, drive);
        if (verdict.isAlreadyApplied()) {
            Web.flashError(request, "You have already applied to " + drive.getCompany().getName() + ".");
            Web.redirect(request, response, "/app/applications");
            return;
        }
        if (!verdict.isEligible()) {
            Web.flashError(request, "You are not eligible for this drive: " + verdict.getFirstReason());
            Web.redirect(request, response, "/app/drive?id=" + drive.getId());
            return;
        }

        try {
            applicationService.apply(profile, drive);
            stats.recordApplication();
            Web.flashSuccess(request, "Applied to " + drive.getCompany().getName()
                    + " for " + drive.getJobRole() + ". The placement cell has been notified.");
        } catch (IllegalStateException ex) {
            Web.flashError(request, ex.getMessage());
        } catch (RuntimeException ex) {
            Web.flashError(request, "That application could not be saved. Please try once more.");
        }
        Web.redirect(request, response, "/app/applications");
    }

    private void withdraw(HttpServletRequest request, HttpServletResponse response,
                          StudentProfile profile) throws IOException {
        Long applicationId = Validators.toLong(request.getParameter("applicationId"));
        try {
            applicationService.withdraw(applicationId, profile.getId());
            Web.flashSuccess(request, "Your application has been withdrawn.");
        } catch (SecurityException ex) {
            Web.flashError(request, "That application does not belong to you.");
        } catch (IllegalStateException ex) {
            Web.flashError(request, ex.getMessage());
        }
        Web.redirect(request, response, "/app/applications");
    }
}
