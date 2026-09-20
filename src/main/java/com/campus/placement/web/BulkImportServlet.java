package com.campus.placement.web;

import com.campus.placement.ejb.AuthService;
import com.campus.placement.util.Branches;
import com.campus.placement.util.Validators;
import jakarta.ejb.EJB;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bulk student import, written with non blocking I/O.
 *
 * <p>The usual {@code request.getInputStream().read()} blocks a container
 * thread until the next byte arrives, so a slow uploader ties up a thread doing
 * nothing. Here the request is put into asynchronous mode and a
 * {@link ReadListener} is registered instead. The container calls
 * {@code onDataAvailable} only when bytes are actually ready, the loop drains
 * whatever {@code isReady()} allows, and the thread is released between
 * callbacks.</p>
 *
 * <p>The response is written and the exchange finished inside
 * {@code onAllDataRead}, and {@code asyncContext.complete()} is what tells the
 * container the request is genuinely over.</p>
 */
@WebServlet(name = "BulkImportServlet", urlPatterns = {"/admin/import"}, asyncSupported = true)
public class BulkImportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BulkImportServlet.class.getName());
    private static final int MAX_BYTES = 512 * 1024;
    private static final String DEFAULT_PASSWORD = "Campus@2026";

    @EJB
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("csrf", Web.csrfToken(request.getSession()));
        request.setAttribute("branches", Branches.ALL);
        request.setAttribute("defaultPassword", DEFAULT_PASSWORD);
        Web.render(request, response, "admin-import.jsp", "import");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        AsyncContext async = request.startAsync();
        async.setTimeout(30_000L);

        ServletInputStream in = request.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        in.setReadListener(new ReadListener() {

            private final byte[] chunk = new byte[4096];

            @Override
            public void onDataAvailable() throws IOException {
                // Only read while the container says data is ready. The moment
                // isReady() turns false the thread goes back to the pool and the
                // container calls this method again when more bytes land.
                while (in.isReady()) {
                    int read = in.read(chunk);
                    if (read < 0) {
                        return;
                    }
                    if (buffer.size() + read > MAX_BYTES) {
                        throw new IOException("The import is larger than the 512 KB limit");
                    }
                    buffer.write(chunk, 0, read);
                }
            }

            @Override
            public void onAllDataRead() throws IOException {
                String csv = buffer.toString(StandardCharsets.UTF_8);
                ImportReport report = importRows(csv);
                try (PrintWriter out = async.getResponse().getWriter()) {
                    out.print(report.toJson());
                }
                async.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                LOG.log(Level.WARNING, "The bulk import stream failed", throwable);
                try {
                    HttpServletResponse res = (HttpServletResponse) async.getResponse();
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    res.getWriter().print("{\"created\":0,\"skipped\":[\"The upload could not be read: "
                            + jsonEscape(String.valueOf(throwable.getMessage())) + "\"]}");
                } catch (IOException ignored) {
                    // The client is already gone.
                } finally {
                    async.complete();
                }
            }
        });
    }

    private ImportReport importRows(String csv) {
        ImportReport report = new ImportReport();
        String[] lines = csv.split("\\r?\\n");
        int rowNumber = 0;

        for (String line : lines) {
            rowNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (rowNumber == 1 && trimmed.toLowerCase().startsWith("name")) {
                continue;
            }

            String[] cells = trimmed.split(",", -1);
            if (cells.length < 7) {
                report.skip(rowNumber, "expected seven columns, found " + cells.length);
                continue;
            }

            String fullName = cells[0].trim();
            String email = Validators.normaliseEmail(cells[1]);
            String rollNo = cells[2].trim();
            String branch = cells[3].trim();
            int batchYear = Validators.toInt(cells[4], -1);
            double cgpa = Validators.toDouble(cells[5], -1);
            int backlogs = Validators.toInt(cells[6], -1);
            String phone = cells.length > 7 ? cells[7].trim() : "";

            int currentYear = Year.now().getValue();
            if (!Validators.isName(fullName)) {
                report.skip(rowNumber, "name is not usable");
            } else if (!Validators.isEmail(email)) {
                report.skip(rowNumber, "\"" + email + "\" is not a valid email address");
            } else if (!Validators.isRollNo(rollNo)) {
                report.skip(rowNumber, "roll number is not usable");
            } else if (!Branches.isKnown(branch)) {
                report.skip(rowNumber, "\"" + branch + "\" is not a known branch");
            } else if (batchYear < currentYear - 4 || batchYear > currentYear + 6) {
                report.skip(rowNumber, "graduating year is out of range");
            } else if (!Validators.inRange(cgpa, 0, 10)) {
                report.skip(rowNumber, "CGPA is out of range");
            } else if (backlogs < 0 || backlogs > 30) {
                report.skip(rowNumber, "backlog count is out of range");
            } else {
                try {
                    authService.registerStudent(email, DEFAULT_PASSWORD, fullName, rollNo,
                            Branches.canonical(branch), batchYear, cgpa, backlogs,
                            Validators.isPhone(phone) ? phone : null);
                    report.created(rollNo);
                } catch (IllegalArgumentException ex) {
                    report.skip(rowNumber, ex.getMessage());
                } catch (RuntimeException ex) {
                    report.skip(rowNumber, "could not be saved");
                }
            }
        }
        return report;
    }

    private static String jsonEscape(String value) {
        return value == null ? ""
                : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    /** Small result carrier, rendered as JSON for the page script. */
    private static final class ImportReport {

        private final List<String> createdRolls = new ArrayList<>();
        private final List<String> skipped = new ArrayList<>();

        void created(String rollNo) {
            createdRolls.add(rollNo);
        }

        void skip(int row, String reason) {
            skipped.add("Row " + row + ": " + reason);
        }

        String toJson() {
            StringBuilder json = new StringBuilder("{\"created\":")
                    .append(createdRolls.size())
                    .append(",\"rolls\":[");
            for (int i = 0; i < createdRolls.size(); i++) {
                json.append(i == 0 ? "" : ",").append('"')
                        .append(jsonEscape(createdRolls.get(i))).append('"');
            }
            json.append("],\"skipped\":[");
            for (int i = 0; i < skipped.size(); i++) {
                json.append(i == 0 ? "" : ",").append('"')
                        .append(jsonEscape(skipped.get(i))).append('"');
            }
            return json.append("]}").toString();
        }
    }
}
