<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Classroom JDBC" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Classroom JDBC</h1>
        <p class="lede">The database code from the practical sessions, running against this project's
            MySQL. Save a mock interview score with either method and watch the row arrive below.</p>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h3>Latest mock scores</h3>
                <div class="spacer"></div>
                <span class="badge badge-neutral">table mock_score</span>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty scores}">
                        <div class="empty">
                            <p class="mb-0">Nothing saved yet. The table is created by the first save
                                made with the classroom method.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table>
                                <thead>
                                <tr><th>#</th><th>Student</th><th>Aptitude</th><th>Technical</th>
                                    <th>Interview</th><th>Total</th></tr>
                                </thead>
                                <tbody>
                                <c:forEach var="s" items="${scores}">
                                    <tr>
                                        <td class="num">${s.id()}</td>
                                        <td><c:out value="${s.studentName()}"/></td>
                                        <td class="num">${s.aptitude()}</td>
                                        <td class="num">${s.technical()}</td>
                                        <td class="num">${s.interview()}</td>
                                        <td class="num"><strong>${s.total()}</strong></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div class="card mt-2">
            <div class="card-head"><h3>Classroom method</h3>
                <div class="spacer"></div><span class="small muted">MockScoreBean.addScore</span></div>
            <div class="card-body">
                <pre><code>Class.forName("com.mysql.cj.jdbc.Driver");
Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
Statement st = con.createStatement();
String query = "insert into mock_score(sname,aptitude,technical,interview) values('"
        + sname + "'  ," + m1 + "  ," + m2 + "  ," + m3 + ")";
st.executeUpdate(query);</code></pre>
                <ul class="small mb-0 mt-1">
                    <li>The URL, user and password are written in the source.</li>
                    <li>A new connection is opened on every call and never closed.</li>
                    <li>The SQL is built by joining strings, so a quote in the name changes the statement.</li>
                    <li>An error is only printed, and the caller is never told the insert failed.</li>
                </ul>
            </div>
        </div>

        <div class="card mt-2">
            <div class="card-head"><h3>Pooled method</h3>
                <div class="spacer"></div><span class="small muted">MockScoreBean.addScoreSafely</span></div>
            <div class="card-body">
                <pre><code>try (Connection con = pooled();
     PreparedStatement ps = con.prepareStatement(
         "insert into mock_score(sname,aptitude,technical,interview) values(?,?,?,?)")) {
    ps.setString(1, sname);
    ps.setInt(2, m1);  ps.setInt(3, m2);  ps.setInt(4, m3);
    ps.executeUpdate();
}</code></pre>
                <ul class="small mb-0 mt-1">
                    <li>The connection comes from the pool the server manages, by its JNDI name.</li>
                    <li>Values are bound to placeholders, so they can never become SQL.</li>
                    <li>try with resources closes the statement and returns the connection.</li>
                    <li>A failure is thrown, so the page can say the save did not happen.</li>
                </ul>
            </div>
        </div>
    </div>

    <div>
        <div class="card">
            <div class="card-head"><h3>Save a score</h3></div>
            <div class="card-body">
                <form method="post" action="${ctx}/admin/classroom-jdbc" novalidate>
                    <input type="hidden" name="csrf" value="${csrf}">

                    <div class="field">
                        <label for="sname">Student name</label>
                        <input type="text" id="sname" name="sname" maxlength="120" required
                               placeholder="Aarti Deshpande">
                    </div>
                    <div class="field">
                        <label for="aptitude">Aptitude (0 to 100)</label>
                        <input type="number" id="aptitude" name="aptitude" min="0" max="100" required value="70">
                    </div>
                    <div class="field">
                        <label for="technical">Technical (0 to 100)</label>
                        <input type="number" id="technical" name="technical" min="0" max="100" required value="75">
                    </div>
                    <div class="field">
                        <label for="interview">Interview (0 to 100)</label>
                        <input type="number" id="interview" name="interview" min="0" max="100" required value="80">
                    </div>

                    <div class="field">
                        <label for="mode">Method</label>
                        <select id="mode" name="mode">
                            <option value="classroom">Classroom (DriverManager and Statement)</option>
                            <option value="pooled">Pooled (DataSource and PreparedStatement)</option>
                        </select>
                    </div>

                    <button type="submit" class="btn btn-primary">Save score</button>
                </form>
                <p class="small muted mt-2 mb-0">Every value is checked in the servlet before the bean is
                    called, because the classroom method is unsafe by design and exists to be read, not trusted.</p>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
