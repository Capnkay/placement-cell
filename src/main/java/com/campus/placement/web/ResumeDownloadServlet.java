package com.campus.placement.web;

import com.campus.placement.ejb.StudentService;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.util.UploadStore;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves a stored resume back to the browser.
 *
 * <p>Two rules make this safe. The file is looked up by the profile id and read
 * from the record, never from a name in the query string, so there is no path
 * for a caller to name an arbitrary file. And a student may only fetch their
 * own resume, while the placement officer may fetch any, which is checked here
 * rather than assumed from the link that was clicked.</p>
 */
@WebServlet(name = "ResumeDownloadServlet", urlPatterns = {"/app/resume"})
public class ResumeDownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private StudentService studentService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SessionUser user = Web.current(request);
        Long requested = Validators.toLong(request.getParameter("student"));
        Long profileId = requested != null ? requested : user.getStudentProfileId();

        if (profileId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No resume on file");
            return;
        }
        if (!user.isAdmin() && !profileId.equals(user.getStudentProfileId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "That resume belongs to another student");
            return;
        }

        StudentProfile profile = studentService.find(profileId);
        if (profile == null || profile.getResumeFile() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No resume has been uploaded yet");
            return;
        }

        Path file = UploadStore.resolve(profile.getResumeFile());
        if (!Files.isReadable(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The stored file is missing");
            return;
        }

        String downloadName = "Resume-" + profile.getRollNo()
                + UploadStore.extensionOf(profile.getResumeFile());

        response.reset();
        response.setContentType(UploadStore.contentTypeFor(profile.getResumeFile()));
        response.setContentLengthLong(Files.size(file));
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + Web.safeHeaderValue(downloadName) + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");

        try (InputStream in = Files.newInputStream(file);
             ServletOutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }
}
