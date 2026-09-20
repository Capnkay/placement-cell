package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.DriveService;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.ApplicationStatus;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.JobApplication;
import com.campus.placement.entity.StudentProfile;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The student landing page. It answers three questions at a glance: what have I
 * applied to, where has each application reached, and which open drives am I
 * actually eligible for today.
 */
@WebServlet(name = "StudentDashboardServlet", urlPatterns = {"/app/dashboard"})
public class StudentDashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StudentService studentService;

    @EJB
    private DriveService driveService;

    @EJB
    private ApplicationService applicationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        if (user.isAdmin()) {
            Web.redirect(request, response, "/admin/dashboard");
            return;
        }

        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.flashError(request, "No student record is linked to this account.");
            Web.redirect(request, response, "/logout");
            return;
        }

        List<JobApplication> applications = applicationService.forStudent(profile.getId());

        Map<ApplicationStatus, Long> mine = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            mine.put(status, applications.stream().filter(a -> a.getStatus() == status).count());
        }

        List<Drive> eligibleOpen = new ArrayList<>();
        for (Drive drive : driveService.listOpen()) {
            if (driveService.check(profile, drive).isCanApply()) {
                eligibleOpen.add(drive);
            }
        }

        boolean placed = applications.stream()
                .anyMatch(a -> a.getStatus() == ApplicationStatus.SELECTED);

        request.setAttribute("profile", profile);
        request.setAttribute("applications", applications);
        request.setAttribute("recentApplications",
                applications.size() > 5 ? applications.subList(0, 5) : applications);
        request.setAttribute("statusCounts", mine);
        request.setAttribute("eligibleOpen", eligibleOpen);
        request.setAttribute("openCount", driveService.countOpen());
        request.setAttribute("placed", placed);
        request.setAttribute("profileComplete", profile.getResumeFile() != null
                && profile.getPhone() != null && !profile.getPhone().isBlank());

        Web.render(request, response, "student-dashboard.jsp", "dashboard");
    }
}
