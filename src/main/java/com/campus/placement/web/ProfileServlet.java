package com.campus.placement.web;

import com.campus.placement.ejb.AuthService;
import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.util.Branches;
import com.campus.placement.util.UploadStore;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The student's own record: academic details, password change and the resume
 * upload. The upload is the file handling half of the syllabus, handled with
 * {@code @MultipartConfig} and the {@link Part} API rather than a third party
 * parser.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = {"/app/profile"})
@MultipartConfig(fileSizeThreshold = 512 * 1024,
        maxFileSize = 2 * 1024 * 1024,
        maxRequestSize = 3 * 1024 * 1024)
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StudentService studentService;

    @EJB
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        SessionUser user = Web.current(request);
        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.redirect(request, response, "/logout");
            return;
        }

        String action = Validators.trim(request.getParameter("action"));
        switch (action) {
            case "details" -> saveDetails(request, response, profile);
            case "password" -> changePassword(request, response, user);
            case "resume" -> uploadResume(request, response, profile);
            default -> Web.redirect(request, response, "/app/profile");
        }
    }

    private void saveDetails(HttpServletRequest request, HttpServletResponse response,
                             StudentProfile profile) throws ServletException, IOException {

        Map<String, String> errors = new LinkedHashMap<>();
        String branch = Validators.trim(request.getParameter("branch"));
        int batchYear = Validators.toInt(request.getParameter("batchYear"), -1);
        double cgpa = Validators.toDouble(request.getParameter("cgpa"), -1);
        int backlogs = Validators.toInt(request.getParameter("backlogs"), -1);
        String phone = Validators.trim(request.getParameter("phone"));

        int currentYear = Year.now().getValue();
        if (!Branches.isKnown(branch)) {
            errors.put("branch", "Choose your branch from the list.");
        }
        if (batchYear < currentYear - 4 || batchYear > currentYear + 6) {
            errors.put("batchYear", "Graduating year looks wrong.");
        }
        if (!Validators.inRange(cgpa, 0, 10)) {
            errors.put("cgpa", "CGPA must be between 0 and 10.");
        }
        if (backlogs < 0 || backlogs > 30) {
            errors.put("backlogs", "Enter zero if you have no live backlogs.");
        }
        if (!Validators.isPhone(phone)) {
            errors.put("phone", "Enter a ten digit mobile number.");
        }

        if (!errors.isEmpty()) {
            show(request, response, errors, null);
            return;
        }

        studentService.updateProfile(profile.getId(), Branches.canonical(branch),
                batchYear, cgpa, backlogs, phone);
        Web.flashSuccess(request, "Your details have been updated. "
                + "Eligibility on every open drive has been recalculated.");
        Web.redirect(request, response, "/app/profile");
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response,
                                SessionUser user) throws ServletException, IOException {

        Map<String, String> errors = new LinkedHashMap<>();
        String current = request.getParameter("currentPassword");
        String next = request.getParameter("newPassword");
        String confirm = request.getParameter("confirmPassword");

        if (Validators.isBlank(current)) {
            errors.put("currentPassword", "Enter your current password.");
        }
        if (!Validators.isStrongPassword(next)) {
            errors.put("newPassword", "Use at least eight characters with letters and digits.");
        } else if (!next.equals(confirm)) {
            errors.put("confirmPassword", "The two entries do not match.");
        }
        if (errors.isEmpty() && !authService.changePassword(user.getId(), current, next)) {
            errors.put("currentPassword", "That is not your current password.");
        }

        if (!errors.isEmpty()) {
            show(request, response, null, errors);
            return;
        }

        Web.flashSuccess(request, "Your password has been changed.");
        Web.redirect(request, response, "/app/profile");
    }

    private void uploadResume(HttpServletRequest request, HttpServletResponse response,
                              StudentProfile profile) throws ServletException, IOException {

        Part part;
        try {
            part = request.getPart("resume");
        } catch (IllegalStateException ex) {
            Web.flashError(request, "That file is larger than the two megabyte limit.");
            Web.redirect(request, response, "/app/profile");
            return;
        }

        if (part == null || part.getSize() == 0) {
            Web.flashError(request, "Choose a file before uploading.");
            Web.redirect(request, response, "/app/profile");
            return;
        }

        String submitted = part.getSubmittedFileName();
        if (!UploadStore.isAllowedResume(submitted)) {
            Web.flashError(request, "Resumes must be a PDF, DOC or DOCX file.");
            Web.redirect(request, response, "/app/profile");
            return;
        }

        String stored = UploadStore.newFileName(profile.getId(), submitted);
        Path target = UploadStore.directory().resolve(stored);

        // The extension has been checked, but an extension is only a string. The
        // first bytes of the file are read and compared against the signature the
        // format is supposed to start with, so something renamed to .pdf is
        // refused before any of it is written to disk.
        try (BufferedInputStream in = new BufferedInputStream(part.getInputStream())) {
            in.mark(UploadStore.SNIFF_BYTES + 1);
            byte[] header = in.readNBytes(UploadStore.SNIFF_BYTES);
            in.reset();

            if (!UploadStore.contentMatches(header, UploadStore.extensionOf(submitted))) {
                Web.flashError(request, "That file does not look like a "
                        + UploadStore.extensionOf(submitted) + " document inside, "
                        + "whatever it is named. Upload the real file.");
                Web.redirect(request, response, "/app/profile");
                return;
            }

            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String previous = profile.getResumeFile();
        studentService.attachResume(profile.getId(), stored);
        if (previous != null && !previous.equals(stored)) {
            UploadStore.deleteQuietly(previous);
        }

        Web.flashSuccess(request, "Resume uploaded. The placement cell can now download it.");
        Web.redirect(request, response, "/app/profile");
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      Map<String, String> detailErrors, Map<String, String> passwordErrors)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        StudentProfile profile = studentService.find(user.getStudentProfileId());
        if (profile == null) {
            Web.redirect(request, response, "/logout");
            return;
        }

        request.setAttribute("profile", profile);
        request.setAttribute("branches", Branches.ALL);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));
        request.setAttribute("detailErrors", detailErrors);
        request.setAttribute("passwordErrors", passwordErrors);

        Web.render(request, response, "profile.jsp", "profile");
    }
}
