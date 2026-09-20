package com.campus.placement.web;

import com.campus.placement.ejb.DriveService;
import com.campus.placement.entity.Company;
import com.campus.placement.entity.Drive;
import com.campus.placement.entity.DriveStatus;
import com.campus.placement.util.Branches;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drive management for the placement officer: announce a drive, edit its rule
 * set, open or close it, remove it.
 *
 * <p>The eligibility numbers are validated as a set, not one by one. A minimum
 * CGPA above ten or a last date after the drive date would both leave a rule
 * that no student could ever satisfy, so both are refused here.</p>
 */
@WebServlet(name = "AdminDriveServlet", urlPatterns = {"/admin/drives", "/admin/drive"})
public class AdminDriveServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private DriveService driveService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long editId = Validators.toLong(request.getParameter("edit"));
        request.setAttribute("editing", editId == null ? null : driveService.find(editId));
        show(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = Validators.trim(request.getParameter("action"));
        Long id = Validators.toLong(request.getParameter("id"));

        switch (action) {
            case "delete" -> {
                Drive drive = driveService.find(id);
                if (drive == null) {
                    Web.flashError(request, "That drive no longer exists.");
                } else {
                    long applications = driveService.countApplicationsFor(id);
                    driveService.delete(id);
                    Web.flashSuccess(request, drive.getCompany().getName() + " drive removed"
                            + (applications > 0 ? ", along with " + applications + " application(s)." : "."));
                }
                Web.redirect(request, response, "/admin/drives");
                return;
            }
            case "open", "close" -> {
                driveService.setStatus(id, "open".equals(action) ? DriveStatus.OPEN : DriveStatus.CLOSED);
                Web.flashSuccess(request, "open".equals(action)
                        ? "The drive is open for applications again."
                        : "The drive has been closed, no further applications will be accepted.");
                Web.redirect(request, response, "/admin/drives");
                return;
            }
            default -> save(request, response, id);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, Long id)
            throws ServletException, IOException {

        Map<String, String> errors = new LinkedHashMap<>();

        Long companyId = Validators.toLong(request.getParameter("companyId"));
        String jobRole = Validators.trim(request.getParameter("jobRole"));
        String location = Validators.trim(request.getParameter("location"));
        double pkg = Validators.toDouble(request.getParameter("packageLpa"), -1);
        double minCgpa = Validators.toDouble(request.getParameter("minCgpa"), -1);
        int maxBacklogs = Validators.toInt(request.getParameter("maxBacklogs"), -1);
        String[] branches = request.getParameterValues("branches");
        String driveDateRaw = Validators.trim(request.getParameter("driveDate"));
        String lastDateRaw = Validators.trim(request.getParameter("lastDate"));
        String description = Validators.trim(request.getParameter("description"));

        Company company = driveService.findCompany(companyId);
        if (company == null) {
            errors.put("companyId", "Choose the recruiting company.");
        }
        if (jobRole.length() < 2 || jobRole.length() > 120) {
            errors.put("jobRole", "Enter the role being offered.");
        }
        if (location.isEmpty() || location.length() > 100) {
            errors.put("location", "Enter the posting location.");
        }
        if (!Validators.inRange(pkg, 0, 200)) {
            errors.put("packageLpa", "Package must be a number of lakhs between 0 and 200.");
        }
        if (!Validators.inRange(minCgpa, 0, 10)) {
            errors.put("minCgpa", "Minimum CGPA must be between 0 and 10.");
        }
        if (maxBacklogs < 0 || maxBacklogs > 30) {
            errors.put("maxBacklogs", "Enter the number of backlogs allowed, zero for none.");
        }

        List<String> chosen = new ArrayList<>();
        if (branches != null) {
            for (String branch : branches) {
                if (Branches.isKnown(branch)) {
                    chosen.add(Branches.canonical(branch));
                }
            }
        }
        if (chosen.isEmpty()) {
            errors.put("branches", "Select at least one eligible branch.");
        }

        LocalDate driveDate = parseDate(driveDateRaw);
        LocalDate lastDate = parseDate(lastDateRaw);
        if (driveDate == null) {
            errors.put("driveDate", "Pick the date the drive is held.");
        }
        if (lastDate == null) {
            errors.put("lastDate", "Pick the last date to apply.");
        }
        if (driveDate != null && lastDate != null && lastDate.isAfter(driveDate)) {
            errors.put("lastDate", "Applications must close on or before the drive date.");
        }
        if (description.length() > 2000) {
            errors.put("description", "Keep the description under two thousand characters.");
        }

        if (!errors.isEmpty()) {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("id", id == null ? "" : String.valueOf(id));
            form.put("companyId", companyId == null ? "" : String.valueOf(companyId));
            form.put("jobRole", jobRole);
            form.put("location", location);
            form.put("packageLpa", Validators.trim(request.getParameter("packageLpa")));
            form.put("minCgpa", Validators.trim(request.getParameter("minCgpa")));
            form.put("maxBacklogs", Validators.trim(request.getParameter("maxBacklogs")));
            form.put("driveDate", driveDateRaw);
            form.put("lastDate", lastDateRaw);
            form.put("description", description);
            form.put("branches", String.join(",", chosen));

            request.setAttribute("editing", id == null ? null : driveService.find(id));
            show(request, response, errors, form);
            return;
        }

        Drive drive = id == null ? new Drive() : driveService.find(id);
        if (drive == null) {
            Web.flashError(request, "That drive could not be found.");
            Web.redirect(request, response, "/admin/drives");
            return;
        }

        drive.setCompany(company);
        drive.setJobRole(jobRole);
        drive.setLocation(location);
        drive.setPackageLpa(pkg);
        drive.setMinCgpa(minCgpa);
        drive.setMaxBacklogs(maxBacklogs);
        drive.setAllowedBranches(String.join(",", chosen));
        drive.setDriveDate(driveDate);
        drive.setLastDate(lastDate);
        drive.setDescription(description.isEmpty() ? null : description);
        if (drive.getStatus() == null) {
            drive.setStatus(DriveStatus.OPEN);
        }
        driveService.save(drive);

        Web.flashSuccess(request, id == null
                ? "Drive announced for " + company.getName() + "."
                : "The " + company.getName() + " drive has been updated.");
        Web.redirect(request, response, "/admin/drives");
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      Map<String, String> errors, Map<String, String> form)
            throws ServletException, IOException {

        List<Drive> drives = driveService.listAll();
        Map<Long, Long> counts = new LinkedHashMap<>();
        drives.forEach(d -> counts.put(d.getId(), driveService.countApplicationsFor(d.getId())));

        request.setAttribute("drives", drives);
        request.setAttribute("applicationCounts", counts);
        request.setAttribute("companies", driveService.listCompanies());
        request.setAttribute("branches", Branches.ALL);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));
        request.setAttribute("errors", errors);
        request.setAttribute("form", form);

        Web.render(request, response, "admin-drives.jsp", "drives");
    }
}
