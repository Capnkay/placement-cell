package com.campus.placement.web;

import com.campus.placement.ejb.DriveService;
import com.campus.placement.jdbc.PlacementReportDao;
import com.campus.placement.util.Branches;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The reporting console, and the page where database connectivity is on show.
 *
 * <p>Everything rendered here came back through plain JDBC rather than through
 * the ORM: the aggregates, the filtered candidate search, the row counts and the
 * connection metadata. The page deliberately prints the SQL beside the results
 * so the query and its output can be read together.</p>
 */
@WebServlet(name = "ReportsServlet", urlPatterns = {"/admin/reports"})
public class ReportsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private PlacementReportDao reportDao;

    @EJB
    private DriveService driveService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String branch = Validators.trim(request.getParameter("branch"));
        String cgpaRaw = Validators.trim(request.getParameter("minCgpa"));
        String backlogRaw = Validators.trim(request.getParameter("maxBacklogs"));
        boolean onlyUnplaced = "on".equals(request.getParameter("onlyUnplaced"));

        // Blank means "no filter", which is different from zero. Null is used for
        // absent so that a deliberate 0 backlogs filter still reaches the query.
        Double minCgpa = cgpaRaw.isEmpty() ? null : Validators.toDouble(cgpaRaw, -1);
        Integer maxBacklogs = backlogRaw.isEmpty() ? null : Validators.toInt(backlogRaw, -1);
        if (minCgpa != null && !Validators.inRange(minCgpa, 0, 10)) {
            minCgpa = null;
        }
        if (maxBacklogs != null && (maxBacklogs < 0 || maxBacklogs > 30)) {
            maxBacklogs = null;
        }
        String searchBranch = Branches.isKnown(branch) ? Branches.canonical(branch) : null;

        try {
            request.setAttribute("connection", reportDao.describeConnection());
            request.setAttribute("branchRows", reportDao.branchReport());
            request.setAttribute("companyRows", reportDao.companyReport());
            request.setAttribute("candidates",
                    reportDao.searchCandidates(searchBranch, minCgpa, maxBacklogs, onlyUnplaced));
            request.setAttribute("appliedFilters",
                    reportDao.explainSearch(searchBranch, minCgpa, maxBacklogs, onlyUnplaced));

            Map<String, Long> counts = new LinkedHashMap<>();
            for (String table : new String[]{"app_user", "student_profile", "company",
                    "drive", "job_application", "audit_log"}) {
                counts.put(table, reportDao.countRows(table));
            }
            request.setAttribute("rowCounts", counts);

        } catch (SQLException ex) {
            request.setAttribute("sqlError", ex.getMessage());
        }

        request.setAttribute("branchSql", PlacementReportDao.BRANCH_SQL);
        request.setAttribute("companySql", PlacementReportDao.COMPANY_SQL);
        request.setAttribute("branches", Branches.ALL);
        request.setAttribute("drives", driveService.listAll());
        request.setAttribute("filterBranch", branch);
        request.setAttribute("filterCgpa", cgpaRaw);
        request.setAttribute("filterBacklogs", backlogRaw);
        request.setAttribute("onlyUnplaced", onlyUnplaced);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "admin-reports.jsp", "reports");
    }

    /**
     * Runs the two statement transaction. Either the drive closes and its
     * pending applications are rejected, or neither happens.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Long driveId = Validators.toLong(request.getParameter("driveId"));
        if (driveId == null) {
            Web.flashError(request, "Choose a drive to close.");
            Web.redirect(request, response, "/admin/reports");
            return;
        }

        try {
            int rejected = reportDao.closeDriveAndRejectPending(driveId);
            Web.flashSuccess(request, "Committed in one transaction: the drive is closed and "
                    + rejected + " pending application(s) were rejected.");
        } catch (SQLException ex) {
            Web.flashError(request, "The transaction was rolled back, nothing changed: "
                    + ex.getMessage());
        }
        Web.redirect(request, response, "/admin/reports");
    }
}
