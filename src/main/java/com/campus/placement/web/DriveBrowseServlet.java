package com.campus.placement.web;

import com.campus.placement.ejb.DriveService;
import com.campus.placement.ejb.Eligibility;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The drive board a student sees. Each row carries its own eligibility verdict,
 * worked out by the session bean rather than by the page, so the button and the
 * explanation beside it can never disagree.
 */
@WebServlet(name = "DriveBrowseServlet", urlPatterns = {"/app/drives", "/app/drive"})
public class DriveBrowseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private DriveService driveService;

    @EJB
    private StudentService studentService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.flashError(request, "No student record is linked to this account.");
            Web.redirect(request, response, "/logout");
            return;
        }

        String path = request.getServletPath();
        if ("/app/drive".equals(path)) {
            showOne(request, response, profile);
            return;
        }

        String filter = Validators.trim(request.getParameter("show"));
        List<Drive> drives = "all".equals(filter) ? driveService.listAll() : driveService.listOpen();

        Map<Long, Eligibility> verdicts = new LinkedHashMap<>();
        for (Drive drive : drives) {
            verdicts.put(drive.getId(), driveService.check(profile, drive));
        }

        long eligibleCount = verdicts.values().stream().filter(Eligibility::isCanApply).count();

        request.setAttribute("profile", profile);
        request.setAttribute("drives", drives);
        request.setAttribute("verdicts", verdicts);
        request.setAttribute("filter", "all".equals(filter) ? "all" : "open");
        request.setAttribute("eligibleCount", eligibleCount);

        Web.render(request, response, "drives.jsp", "drives");
    }

    private void showOne(HttpServletRequest request, HttpServletResponse response,
                         StudentProfile profile) throws ServletException, IOException {

        Drive drive = driveService.find(Validators.toLong(request.getParameter("id")));
        if (drive == null) {
            Web.flashError(request, "That drive is no longer on the board.");
            Web.redirect(request, response, "/app/drives");
            return;
        }

        request.setAttribute("profile", profile);
        request.setAttribute("drive", drive);
        request.setAttribute("verdict", driveService.check(profile, drive));
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "drive-detail.jsp", "drives");
    }
}
