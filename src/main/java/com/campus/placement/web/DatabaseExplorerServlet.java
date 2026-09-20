package com.campus.placement.web;

import com.campus.placement.jdbc.DatabaseExplorerDao;
import com.campus.placement.jdbc.PlacementReportDao;
import com.campus.placement.jdbc.ResultTable;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * The database, shown inside the application.
 *
 * <p>Three views of the same connection: the schema as the database reports it,
 * the rows in any table a page at a time, and a console for read only
 * statements. Officer only, because {@code /admin/} is behind the role check in
 * {@code AuthFilter}.</p>
 */
@WebServlet(name = "DatabaseExplorerServlet", urlPatterns = {"/admin/database"})
public class DatabaseExplorerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private DatabaseExplorerDao explorer;

    @EJB
    private PlacementReportDao reportDao;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response, null);
    }

    /** Runs a statement from the console and renders the result on the same page. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String sql = request.getParameter("sql");

        ResultTable result = explorer.runReadOnly(sql);
        request.setAttribute("submittedSql", sql);
        show(request, response, result);
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      ResultTable queryResult) throws ServletException, IOException {

        String table = Validators.trim(request.getParameter("table"));
        int page = Math.max(0, Validators.toInt(request.getParameter("page"), 0));
        String tab = Validators.trim(request.getParameter("tab"));
        if (tab.isEmpty()) {
            tab = queryResult != null ? "query" : "schema";
        }

        try {
            Map<String, Long> tables = explorer.listTables();
            request.setAttribute("tables", tables);

            // Default to the first table so the page is never blank on arrival.
            if (table.isEmpty() && !tables.isEmpty()) {
                table = tables.keySet().iterator().next();
            }

            if (!table.isEmpty()) {
                request.setAttribute("detail", explorer.describe(table));
                if ("data".equals(tab)) {
                    request.setAttribute("rows", explorer.browse(table, page));
                }
            }
        } catch (SQLException ex) {
            request.setAttribute("schemaError", ex.getMessage());
        }

        request.setAttribute("connection", reportDao.describeConnection());
        request.setAttribute("selectedTable", table);
        request.setAttribute("page", page);
        request.setAttribute("pageSize", DatabaseExplorerDao.PAGE_SIZE);
        request.setAttribute("maxRows", DatabaseExplorerDao.MAX_ROWS);
        request.setAttribute("timeout", DatabaseExplorerDao.TIMEOUT_SECONDS);
        request.setAttribute("tab", tab);
        request.setAttribute("queryResult", queryResult);
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));

        Web.render(request, response, "admin-database.jsp", "database");
    }
}
