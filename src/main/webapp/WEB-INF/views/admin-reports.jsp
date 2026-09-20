<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Reports" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Reports</h1>
        <p class="lede">Everything on this page was read with plain JDBC rather than through
            the ORM. Reports aggregate across tables and are never written back, which is
            the shape of problem SQL is better at than an object mapping.</p>
    </div>
</div>

<c:if test="${not empty sqlError}">
    <div class="alert alert-bad">
        <svg class="i"><use href="#i-alert"/></svg>
        <div><strong>The database could not be read.</strong>
            <span class="mono"><c:out value="${sqlError}"/></span></div>
    </div>
</c:if>

<div class="card">
    <div class="card-head">
        <svg class="i"><use href="#i-database"/></svg>
        <h2>The live connection</h2>
        <div class="spacer"></div>
        <c:choose>
            <c:when test="${connection.available}"><span class="badge badge-ok">Connected</span></c:when>
            <c:otherwise><span class="badge badge-bad">Unavailable</span></c:otherwise>
        </c:choose>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${connection.available}">
                <dl class="kv">
                    <c:forEach var="e" items="${connection.values}">
                        <dt><c:out value="${e.key}"/></dt>
                        <dd class="mono"><c:out value="${e.value}"/></dd>
                    </c:forEach>
                </dl>
            </c:when>
            <c:otherwise>
                <p class="mb-0"><c:out value="${connection.failure}"/></p>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="card-foot">
        None of this is stored by the application. It comes from
        <span class="mono">DatabaseMetaData</span> on a connection borrowed from the pool
        that <span class="mono">WEB-INF/glassfish-resources.xml</span> published under
        <span class="mono">java:app/jdbc/placementDS</span>, the same name
        <span class="mono">persistence.xml</span> hands to Hibernate.
    </div>
</div>

<c:if test="${not empty rowCounts}">
    <div class="metrics mt-2">
        <c:forEach var="e" items="${rowCounts}">
            <div class="metric">
                <span class="k mono">${e.key}</span>
                <span class="v">${e.value}</span>
                <span class="n">rows</span>
            </div>
        </c:forEach>
    </div>
</c:if>

<div class="card">
    <div class="card-head">
        <h2>Placement by branch</h2>
        <div class="spacer"></div>
        <span class="count">${branchRows.size()}</span>
    </div>
    <c:choose>
        <c:when test="${empty branchRows}">
            <div class="empty">
                <h3>No student records to aggregate</h3>
                <p>Once students are on the register this table fills itself from a single
                    grouped query.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Branch</th><th class="right">Students</th><th class="right">Applications</th>
                        <th class="right">Shortlisted</th><th class="right">Placed</th>
                        <th class="right">Rate</th><th class="right">Avg CGPA</th>
                        <th class="right">Best package</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${branchRows}">
                        <tr>
                            <td class="strong"><c:out value="${r.branch}"/></td>
                            <td class="right num">${r.students}</td>
                            <td class="right num">${r.applications}</td>
                            <td class="right num">${r.shortlisted}</td>
                            <td class="right num">${r.selected}</td>
                            <td class="right num">${r.placementRate}%</td>
                            <td class="right num"><fmt:formatNumber value="${r.averageCgpa}"
                                                                    maxFractionDigits="2"/></td>
                            <td class="right num">
                                <c:choose>
                                    <c:when test="${r.bestPackage == 0}"><span class="faint">none</span></c:when>
                                    <c:otherwise>${r.bestPackage} LPA</c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
    <div class="card-foot">
        <details>
            <summary style="cursor:pointer">The SQL behind this table</summary>
            <pre class="mt-1"><code><c:out value="${branchSql}"/></code></pre>
            <p class="mb-0 mt-1">A LEFT JOIN, so a branch with no applications still appears
                with zeroes. An INNER JOIN would silently drop it and the report would be wrong
                in a way nobody notices.</p>
        </details>
    </div>
</div>

<div class="card">
    <div class="card-head">
        <h2>Recruiter performance</h2>
        <div class="spacer"></div>
        <span class="count">${companyRows.size()}</span>
    </div>
    <c:choose>
        <c:when test="${empty companyRows}">
            <div class="empty">
                <h3>No recruiters on the register</h3>
                <p>Add a company and its drives to see hiring numbers here.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Company</th><th>Sector</th><th class="right">Drives</th>
                        <th class="right">Applications</th><th class="right">Offers</th>
                        <th class="right">Conversion</th><th class="right">Avg package</th>
                        <th class="right">Best</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${companyRows}">
                        <tr>
                            <td class="strong"><c:out value="${r.company}"/></td>
                            <td class="muted"><c:out value="${r.sector}"/></td>
                            <td class="right num">${r.drives}</td>
                            <td class="right num">${r.applications}</td>
                            <td class="right num">${r.selected}</td>
                            <td class="right num">${r.conversionRate}%</td>
                            <td class="right num"><fmt:formatNumber value="${r.averagePackage}"
                                                                    maxFractionDigits="1"/></td>
                            <td class="right num">${r.bestPackage}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
    <div class="card-foot">
        <details>
            <summary style="cursor:pointer">The SQL behind this table</summary>
            <pre class="mt-1"><code><c:out value="${companySql}"/></code></pre>
            <p class="mb-0 mt-1">Correlated subqueries rather than one wide join. Joining
                drives and applications together and then averaging the package would count
                each drive once per application and report the wrong average.</p>
        </details>
    </div>
</div>

<div class="card">
    <div class="card-head"><h2>Candidate search</h2></div>
    <div class="card-body">
        <form method="get" action="${ctx}/admin/reports" class="filter-bar">
            <div class="field">
                <label for="branch">Branch</label>
                <select id="branch" name="branch">
                    <option value="">Any branch</option>
                    <c:forEach var="b" items="${branches}">
                        <option value="<c:out value='${b}'/>" ${filterBranch == b ? 'selected' : ''}>
                            <c:out value="${b}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="minCgpa">Minimum CGPA</label>
                <input type="number" id="minCgpa" name="minCgpa" step="0.01" min="0" max="10"
                       placeholder="any" value="<c:out value='${filterCgpa}'/>">
            </div>
            <div class="field">
                <label for="maxBacklogs">Backlogs at most</label>
                <input type="number" id="maxBacklogs" name="maxBacklogs" min="0" max="30"
                       placeholder="any" value="<c:out value='${filterBacklogs}'/>">
            </div>
            <div class="field">
                <label class="check">
                    <input type="checkbox" name="onlyUnplaced" ${onlyUnplaced ? 'checked' : ''}>
                    <span>Hide students with an offer</span>
                </label>
            </div>
            <button class="btn btn-primary" type="submit">
                <svg class="i i-sm"><use href="#i-search"/></svg> Search</button>
            <a class="btn" href="${ctx}/admin/reports">Reset</a>
        </form>

        <p class="small muted mt-2 mb-0">
            Conditions bound as parameters:
            <span class="mono"><c:out value="${appliedFilters}"/></span>.
            Each one is a <span class="mono">?</span> placeholder with the value set by index,
            so nothing typed into this form is ever read as SQL.
        </p>
    </div>

    <c:choose>
        <c:when test="${empty candidates}">
            <div class="empty">
                <h3>No candidates match those filters</h3>
                <p>Loosen a filter, or <a href="${ctx}/admin/reports">reset the search</a>.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Candidate</th><th>Branch</th><th class="right">CGPA</th>
                        <th class="right">Backlogs</th><th class="right">Applications</th>
                        <th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="r" items="${candidates}">
                        <tr>
                            <td><span class="strong"><c:out value="${r.fullName}"/></span>
                                <span class="sub"><c:out value="${r.rollNo}"/>
                                    <span class="sep">&#183;</span><c:out value="${r.email}"/></span></td>
                            <td><c:out value="${r.branch}"/></td>
                            <td class="right num">${r.cgpa}</td>
                            <td class="right num">${r.backlogs}</td>
                            <td class="right num">${r.applications}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${r.placed}"><span class="badge badge-ok">Placed</span></c:when>
                                    <c:otherwise><span class="badge badge-neutral">Available</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="right">
                                <a class="btn btn-sm" href="${ctx}/admin/student?id=${r.profileId}">Open</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <div class="card-head">
        <svg class="i"><use href="#i-lock"/></svg>
        <h2>Close a drive in one transaction</h2>
    </div>
    <div class="card-body">
        <p class="muted">Closing a drive and rejecting the applications still sitting at the
            applied stage are two statements that have to succeed together. A drive that is
            closed while its pending applications hang is a worse state than either change
            alone, so auto commit is switched off and both are committed at the same instant.
            If the second statement fails, the first is rolled back with it.</p>

        <form method="post" action="${ctx}/admin/reports" class="filter-bar"
              data-confirm="Close this drive and reject every pending application?">
            <input type="hidden" name="csrf" value="${csrf}">
            <div class="field" style="flex:1; min-width:260px">
                <label for="driveId">Drive</label>
                <select id="driveId" name="driveId" required style="width:100%">
                    <option value="">Choose a drive</option>
                    <c:forEach var="d" items="${drives}">
                        <option value="${d.id}">
                            <c:out value="${d.company.name}"/> : <c:out value="${d.jobRole}"/>
                            (${d.status.label})
                        </option>
                    </c:forEach>
                </select>
            </div>
            <button class="btn btn-danger" type="submit">Close and reject pending</button>
        </form>
    </div>
    <div class="card-foot">
        The bean is marked <span class="mono">NOT_SUPPORTED</span> so the container does not
        start a transaction of its own. Driving one by hand with
        <span class="mono">setAutoCommit(false)</span> only means anything when nobody else
        is already managing it.
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
