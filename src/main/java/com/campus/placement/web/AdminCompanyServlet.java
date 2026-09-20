package com.campus.placement.web;

import com.campus.placement.ejb.DriveService;
import com.campus.placement.entity.Company;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The recruiter register. Create, edit and remove companies.
 */
@WebServlet(name = "AdminCompanyServlet", urlPatterns = {"/admin/companies", "/admin/company"})
public class AdminCompanyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private DriveService driveService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long editId = Validators.toLong(request.getParameter("edit"));
        request.setAttribute("editing", editId == null ? null : driveService.findCompany(editId));
        show(request, response, null, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = Validators.trim(request.getParameter("action"));

        if ("delete".equals(action)) {
            Long id = Validators.toLong(request.getParameter("id"));
            Company company = driveService.findCompany(id);
            long driveCount = company == null ? 0 : driveService.countDrivesFor(company.getId());
            if (company == null) {
                Web.flashError(request, "That company is no longer on the register.");
            } else if (driveCount > 0) {
                Web.flashError(request, company.getName() + " still has "
                        + driveCount + " drive(s). Remove those first.");
            } else {
                driveService.deleteCompany(id);
                Web.flashSuccess(request, company.getName() + " has been removed.");
            }
            Web.redirect(request, response, "/admin/companies");
            return;
        }

        Long id = Validators.toLong(request.getParameter("id"));
        String name = Validators.trim(request.getParameter("name"));
        String sector = Validators.trim(request.getParameter("sector"));
        String hrEmail = Validators.normaliseEmail(request.getParameter("hrEmail"));
        String website = Validators.trim(request.getParameter("website"));

        Map<String, String> errors = new LinkedHashMap<>();
        if (name.length() < 2 || name.length() > 120) {
            errors.put("name", "Company name must be between two and one hundred twenty characters.");
        } else if (driveService.companyNameTaken(name, id)) {
            errors.put("name", "A company by that name is already on the register.");
        }
        if (sector.isEmpty() || sector.length() > 80) {
            errors.put("sector", "Enter the sector, for example Product Engineering.");
        }
        if (!Validators.isEmail(hrEmail)) {
            errors.put("hrEmail", "Enter a valid contact address in the form name@example.com");
        }
        if (!website.isEmpty() && !website.matches("^https?://[^\\s]{3,190}$")) {
            errors.put("website", "Website must start with http:// or https://");
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("id", id == null ? "" : String.valueOf(id));
        form.put("name", name);
        form.put("sector", sector);
        form.put("hrEmail", hrEmail);
        form.put("website", website);

        if (!errors.isEmpty()) {
            request.setAttribute("editing", id == null ? null : driveService.findCompany(id));
            show(request, response, errors, form);
            return;
        }

        Company company = id == null ? new Company() : driveService.findCompany(id);
        if (company == null) {
            Web.flashError(request, "That company could not be found.");
            Web.redirect(request, response, "/admin/companies");
            return;
        }
        company.setName(name);
        company.setSector(sector);
        company.setHrEmail(hrEmail);
        company.setWebsite(website.isEmpty() ? null : website);
        driveService.saveCompany(company);

        Web.flashSuccess(request, id == null
                ? name + " has been added to the register."
                : name + " has been updated.");
        Web.redirect(request, response, "/admin/companies");
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      Map<String, String> errors, Map<String, String> form)
            throws ServletException, IOException {
        var companies = driveService.listCompanies();
        Map<Long, Long> driveCounts = new LinkedHashMap<>();
        companies.forEach(c -> driveCounts.put(c.getId(), driveService.countDrivesFor(c.getId())));

        request.setAttribute("companies", companies);
        request.setAttribute("driveCounts", driveCounts);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));
        request.setAttribute("errors", errors);
        request.setAttribute("form", form);
        Web.render(request, response, "admin-companies.jsp", "companies");
    }
}
