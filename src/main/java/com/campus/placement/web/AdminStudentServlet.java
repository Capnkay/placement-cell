package com.campus.placement.web;

import com.campus.placement.ejb.ApplicationService;
import com.campus.placement.ejb.AuthService;
import com.campus.placement.ejb.StudentService;
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
 * The student register as the placement officer sees it: search, open one
 * record, suspend or restore an account, clear a lockout, remove a student.
 */
@WebServlet(name = "AdminStudentServlet", urlPatterns = {"/admin/students", "/admin/student"})
public class AdminStudentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StudentService studentService;

    @EJB
    private ApplicationService applicationService;

    @EJB
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if ("/admin/student".equals(request.getServletPath())) {
            StudentProfile profile = studentService.find(Validators.toLong(request.getParameter("id")));
            if (profile == null) {
                Web.flashError(request, "That student record could not be found.");
                Web.redirect(request, response, "/admin/students");
                return;
            }
            request.setAttribute("profile", profile);
            request.setAttribute("applications", applicationService.forStudent(profile.getId()));
            request.setAttribute("csrf", Web.csrfToken(request.getSession()));
            Web.render(request, response, "admin-student-detail.jsp", "students");
            return;
        }

        String query = Validators.trim(request.getParameter("q"));
        List<StudentProfile> students = query.isEmpty()
                ? studentService.listAll()
                : studentService.search(query);

        request.setAttribute("students", students);
        request.setAttribute("query", query);
        request.setAttribute("placedIds", applicationService.placedStudentIds());
        request.setAttribute("appCounts", applicationService.countsByStudent());
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "admin-students.jsp", "students");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = Validators.trim(request.getParameter("action"));
        Long profileId = Validators.toLong(request.getParameter("id"));
        StudentProfile profile = studentService.find(profileId);

        if (profile == null) {
            Web.flashError(request, "That student record could not be found.");
            Web.redirect(request, response, "/admin/students");
            return;
        }

        String name = profile.getUser().getFullName();
        Long userId = profile.getUser().getId();

        switch (action) {
            case "suspend" -> {
                authService.setActive(userId, false);
                Web.flashSuccess(request, name + " can no longer sign in.");
            }
            case "restore" -> {
                authService.setActive(userId, true);
                Web.flashSuccess(request, name + " can sign in again.");
            }
            case "unlock" -> {
                authService.unlock(userId);
                Web.flashSuccess(request, "The lockout on " + name + " has been cleared.");
            }
            case "delete" -> {
                studentService.delete(profileId);
                Web.flashSuccess(request, name + " has been removed from the register.");
                Web.redirect(request, response, "/admin/students");
                return;
            }
            default -> Web.flashError(request, "That action is not recognised.");
        }

        Web.redirect(request, response, "/admin/student?id=" + profileId);
    }
}
