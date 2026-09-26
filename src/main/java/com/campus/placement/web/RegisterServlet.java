package com.campus.placement.web;

import com.campus.placement.ejb.AuthService;
import com.campus.placement.entity.StudentProfile;
import com.campus.placement.util.Branches;
import com.campus.placement.util.EmailDomainVerifier;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Student self registration.
 *
 * <p>Every field is checked here on the server, not only in the browser. The
 * HTML attributes on the form give quick feedback while typing, but a request
 * built by hand skips all of them, so this servlet repeats each rule and
 * returns the form with a message beside the offending field.</p>
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (Web.current(request) != null) {
            Web.redirect(request, response, "/app/dashboard");
            return;
        }
        prepare(request);
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        Map<String, String> errors = new LinkedHashMap<>();

        String fullName = Validators.trim(request.getParameter("fullName"));
        String email = Validators.normaliseEmail(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirm = request.getParameter("confirm");
        String rollNo = Validators.trim(request.getParameter("rollNo"));
        String branch = Validators.trim(request.getParameter("branch"));
        String batchRaw = Validators.trim(request.getParameter("batchYear"));
        String cgpaRaw = Validators.trim(request.getParameter("cgpa"));
        String backlogsRaw = Validators.trim(request.getParameter("backlogs"));
        String phone = Validators.trim(request.getParameter("phone"));

        if (!Web.csrfValid(request)) {
            errors.put("form", "This page sat open too long to be verified. Please submit it again.");
        }
        if (!Validators.isName(fullName)) {
            errors.put("fullName", "Enter your full name using letters, spaces, apostrophes or hyphens.");
        }
        if (!Validators.isEmail(email)) {
            errors.put("email", "Enter a valid email address in the form name@example.com");
        } else if (EmailDomainVerifier.isDisposableDomain(EmailDomainVerifier.domainOf(email))) {
            errors.put("email", "Please use a permanent email address, not a disposable or temporary inbox.");
        } else if (!EmailDomainVerifier.hasValidMxOrA(EmailDomainVerifier.domainOf(email))) {
            errors.put("email", "That domain doesn't appear to accept email. Check for a typo and try again.");
        } else if (authService.emailTaken(email)) {
            errors.put("email", "An account already exists for that address. Try signing in instead.");
        }
        if (!Validators.isStrongPassword(password)) {
            errors.put("password", "Use at least eight characters with a mix of letters and digits.");
        } else if (!password.equals(confirm)) {
            errors.put("confirm", "The two passwords do not match.");
        }
        if (!Validators.isRollNo(rollNo)) {
            errors.put("rollNo", "Roll numbers are three to thirty characters, letters and digits only.");
        }
        if (!Branches.isKnown(branch)) {
            errors.put("branch", "Choose your branch from the list.");
        }

        int currentYear = Year.now().getValue();
        int batchYear = Validators.toInt(batchRaw, -1);
        if (batchYear < currentYear - 4 || batchYear > currentYear + 6) {
            errors.put("batchYear", "Graduating year must be between "
                    + (currentYear - 4) + " and " + (currentYear + 6) + ".");
        }

        double cgpa = Validators.toDouble(cgpaRaw, -1);
        if (!Validators.inRange(cgpa, 0, 10)) {
            errors.put("cgpa", "CGPA must be a number between 0 and 10.");
        }

        int backlogs = Validators.toInt(backlogsRaw, -1);
        if (backlogs < 0 || backlogs > 30) {
            errors.put("backlogs", "Enter the number of live backlogs, zero if you have none.");
        }
        if (!Validators.isPhone(phone)) {
            errors.put("phone", "Enter a ten digit mobile number.");
        }

        if (!errors.isEmpty()) {
            prepare(request);
            request.setAttribute("errors", errors);
            request.setAttribute("form", snapshot(fullName, email, rollNo, branch,
                    batchRaw, cgpaRaw, backlogsRaw, phone));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        try {
            StudentProfile profile = authService.registerStudent(email, password, fullName,
                    rollNo, branch, batchYear, cgpa, backlogs, phone);
            // The sign in page turns these into a proper welcome, with the email
            // already filled in and the cursor in the password field. They are read
            // once and removed, so a later visit to /login is a plain sign in.
            HttpSession session = request.getSession(true);
            session.setAttribute(Web.SESSION_WELCOME_NAME, fullName);
            session.setAttribute(Web.SESSION_WELCOME_EMAIL, email);
            session.setAttribute(Web.SESSION_WELCOME_ROLL, profile.getRollNo());
            Web.redirect(request, response, "/login");
        } catch (IllegalArgumentException ex) {
            errors.put("form", ex.getMessage());
            prepare(request);
            request.setAttribute("errors", errors);
            request.setAttribute("form", snapshot(fullName, email, rollNo, branch,
                    batchRaw, cgpaRaw, backlogsRaw, phone));
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }

    private void prepare(HttpServletRequest request) {
        request.setAttribute("csrf", Web.csrfToken(request.getSession(true)));
        request.setAttribute("branches", Branches.ALL);
        request.setAttribute("currentYear", Year.now().getValue());
    }

    private Map<String, String> snapshot(String fullName, String email, String rollNo,
                                         String branch, String batchYear, String cgpa,
                                         String backlogs, String phone) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("fullName", fullName);
        form.put("email", email);
        form.put("rollNo", rollNo);
        form.put("branch", branch);
        form.put("batchYear", batchYear);
        form.put("cgpa", cgpa);
        form.put("backlogs", backlogs);
        form.put("phone", phone);
        return form;
    }
}
