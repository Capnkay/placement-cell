<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Database" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Database</h1>
        <p class="lede">The schema as MySQL reports it, the rows in each table, and a console
            for read only statements. Nothing here is hard coded: the tables, columns, keys
            and indexes are all read back from the live connection.</p>
    </div>
    <div class="spacer"></div>
    <c:if test="${connection.available}">
        <p class="small muted mb-0" style="padding-top:6px; text-align:right">
            <span class="badge badge-ok">Connected</span><br>
            <span class="mono faint">${connection.values['Database']}</span>
        </p>
    </c:if>
</div>

<c:if test="${not empty schemaError}">
    <div class="alert alert-bad">
        <svg class="i"><use href="#i-alert"/></svg>
        <div><c:out value="${schemaError}"/></div>
    </div>
</c:if>

<div class="split" style="grid-template-columns: 230px minmax(0, 1fr)">

    <%-- Table list ------------------------------------------------------ --%>
    <div>
        <div class="card">
            <div class="card-head">
                <svg class="i"><use href="#i-table"/></svg>
                <h3>Tables</h3>
                <div class="spacer"></div>
                <span class="count">${tables.size()}</span>
            </div>
            <div class="table-wrap">
                <table>
                    <tbody>
                    <c:forEach var="t" items="${tables}">
                        <tr>
                            <td style="${t.key eq selectedTable ? 'background:var(--accent-quiet)' : ''}">
                                <a href="${ctx}/admin/database?table=${t.key}&amp;tab=${tab}"
                                   class="mono ${t.key eq selectedTable ? 'strong' : ''}">
                                    <c:out value="${t.key}"/></a>
                                <span class="sub">${t.value} row<c:if test="${t.value ne 1}">s</c:if></span>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <%-- Detail ---------------------------------------------------------- --%>
    <div>
        <div class="tabs">
            <a href="${ctx}/admin/database?table=${selectedTable}&amp;tab=schema"
               class="${tab == 'schema' ? 'active' : ''}">Structure</a>
            <a href="${ctx}/admin/database?table=${selectedTable}&amp;tab=data"
               class="${tab == 'data' ? 'active' : ''}">Rows</a>
            <a href="${ctx}/admin/database?table=${selectedTable}&amp;tab=query"
               class="${tab == 'query' ? 'active' : ''}">Query console</a>
        </div>

        <%-- ---------- Structure ---------- --%>
        <c:if test="${tab == 'schema' and not empty detail}">
            <div class="card">
                <div class="card-head">
                    <h2 class="mono"><c:out value="${detail.name}"/></h2>
                    <div class="spacer"></div>
                    <span class="count">${detail.rowCount} rows</span>
                </div>
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr><th>Column</th><th>Type</th><th class="right">Size</th>
                            <th>Null</th><th>Default</th><th>Key</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach var="col" items="${detail.columns}">
                            <tr>
                                <td class="mono strong"><c:out value="${col.name}"/></td>
                                <td class="mono muted"><c:out value="${col.type}"/></td>
                                <td class="right num muted">${col.size}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${col.nullable}"><span class="faint">yes</span></c:when>
                                        <c:otherwise><span class="badge badge-neutral">required</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="mono faint">
                                    <c:out value="${empty col.defaultValue ? '-' : col.defaultValue}"/></td>
                                <td>
                                    <c:if test="${col.primaryKey}">
                                        <span class="badge badge-brand">
                                            <svg class="i i-sm"><use href="#i-key"/></svg> primary</span>
                                    </c:if>
                                    <c:if test="${col.autoIncrement}">
                                        <span class="chip">auto</span></c:if>
                                    <c:if test="${col.masked}">
                                        <span class="badge badge-warn">never shown</span></c:if>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="grid cols-2">
                <div class="card">
                    <div class="card-head">
                        <svg class="i"><use href="#i-link"/></svg>
                        <h3>Relationships</h3>
                    </div>
                    <c:choose>
                        <c:when test="${empty detail.foreignKeys}">
                            <div class="empty">
                                <h3>No foreign keys</h3>
                                <p>This table does not point at any other.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-wrap">
                                <table>
                                    <thead><tr><th>Column</th><th>References</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="fk" items="${detail.foreignKeys}">
                                        <tr>
                                            <td class="mono"><c:out value="${fk.column}"/></td>
                                            <td class="mono">
                                                <a href="${ctx}/admin/database?table=${fk.referencedTable}&amp;tab=schema">
                                                    <c:out value="${fk.referencedTable}"/></a><c:out
                                                    value=".${fk.referencedColumn}"/>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                            <div class="card-foot">
                                These are the JPA relationships as they actually exist in the
                                database. A <span class="mono">@ManyToOne</span> in the entity
                                became the foreign key you are looking at.
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="card">
                    <div class="card-head"><h3>Indexes</h3></div>
                    <c:choose>
                        <c:when test="${empty detail.indexes}">
                            <div class="empty">
                                <h3>No indexes</h3>
                                <p>Not even a primary key, which would be unusual.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-wrap">
                                <table>
                                    <thead><tr><th>Name</th><th>Columns</th><th>Unique</th></tr></thead>
                                    <tbody>
                                    <c:forEach var="idx" items="${detail.indexes}">
                                        <tr>
                                            <td class="mono"><c:out value="${idx.name}"/></td>
                                            <td class="mono muted"><c:out value="${idx.columns}"/></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${idx.unique}">
                                                        <span class="badge badge-ok">unique</span></c:when>
                                                    <c:otherwise><span class="faint">no</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:if>

        <%-- ---------- Rows ---------- --%>
        <c:if test="${tab == 'data'}">
            <div class="card">
                <div class="card-head">
                    <h2 class="mono"><c:out value="${selectedTable}"/></h2>
                    <div class="spacer"></div>
                    <span class="small muted">
                        rows ${page * pageSize + 1} to ${page * pageSize + rows.rowCount}
                        of ${detail.rowCount}
                    </span>
                </div>
                <c:choose>
                    <c:when test="${empty rows or rows.rowCount == 0}">
                        <div class="empty">
                            <h3>No rows on this page</h3>
                            <p>The table is empty, or you have paged past the end of it.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table>
                                <thead>
                                <tr>
                                    <c:forEach var="col" items="${rows.columns}" varStatus="s">
                                        <th class="mono"><c:out value="${col}"/>
                                            <span class="faint" style="font-weight:400">
                                                <c:out value="${rows.types[s.index]}"/></span></th>
                                    </c:forEach>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${rows.rows}">
                                    <tr>
                                        <c:forEach var="cell" items="${row}">
                                            <td class="mono">
                                                <c:choose>
                                                    <c:when test="${cell == null}">
                                                        <span class="faint">NULL</span></c:when>
                                                    <c:when test="${cell == '(hidden)'}">
                                                        <span class="badge badge-warn">hidden</span></c:when>
                                                    <c:otherwise><c:out value="${cell}"/></c:otherwise>
                                                </c:choose>
                                            </td>
                                        </c:forEach>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
                <div class="card-foot" style="display:flex; align-items:center; gap:10px">
                    <span class="mono"><c:out value="${rows.sql}"/></span>
                    <div class="spacer"></div>
                    <c:if test="${page > 0}">
                        <a class="btn btn-sm"
                           href="${ctx}/admin/database?table=${selectedTable}&amp;tab=data&amp;page=${page - 1}">
                            Previous</a>
                    </c:if>
                    <c:if test="${(page + 1) * pageSize < detail.rowCount}">
                        <a class="btn btn-sm"
                           href="${ctx}/admin/database?table=${selectedTable}&amp;tab=data&amp;page=${page + 1}">
                            Next</a>
                    </c:if>
                </div>
            </div>
            <p class="small muted">
                Password digests are replaced before they leave the data access class, so
                there is no view in this application that can show one.
            </p>
        </c:if>

        <%-- ---------- Console ---------- --%>
        <c:if test="${tab == 'query'}">
            <div class="card">
                <div class="card-head">
                    <svg class="i"><use href="#i-play"/></svg>
                    <h2>Query console</h2>
                </div>
                <div class="card-body">
                    <form method="post" action="${ctx}/admin/database">
                        <input type="hidden" name="csrf" value="${csrf}">
                        <input type="hidden" name="table" value="${selectedTable}">
                        <input type="hidden" name="tab" value="query">
                        <div class="field">
                            <label for="sql">Statement</label>
                            <textarea id="sql" name="sql" rows="6" spellcheck="false" class="mono"
                                      placeholder="SELECT branch, COUNT(*) FROM student_profile GROUP BY branch"><c:choose><c:when
                                    test="${empty submittedSql}">SELECT s.roll_no, u.full_name, s.branch, s.cgpa
  FROM student_profile s
  JOIN app_user u ON u.id = s.user_id
 ORDER BY s.cgpa DESC</c:when><c:otherwise><c:out value="${submittedSql}"/></c:otherwise></c:choose></textarea>
                        </div>
                        <div class="btn-row">
                            <button class="btn btn-primary" type="submit">Run</button>
                            <span class="small muted">
                                Read only, at most ${maxRows} rows, cancelled after ${timeout} seconds.
                            </span>
                        </div>
                    </form>
                </div>
                <div class="card-foot">
                    Only SELECT, WITH, SHOW, DESCRIBE and EXPLAIN are accepted, one statement at
                    a time, and the connection is put into read only mode before the statement
                    runs. The keyword check would be a weak defence on its own, which is why the
                    server is told to refuse writes as well.
                </div>
            </div>

            <c:if test="${not empty queryResult}">
                <c:choose>
                    <c:when test="${queryResult.failed}">
                        <div class="alert alert-bad">
                            <svg class="i"><use href="#i-alert"/></svg>
                            <div><strong>Refused.</strong> <c:out value="${queryResult.error}"/></div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="card">
                            <div class="card-head">
                                <h3>Result</h3>
                                <div class="spacer"></div>
                                <span class="small muted">
                                    ${queryResult.rowCount} row<c:if test="${queryResult.rowCount ne 1}">s</c:if>
                                    <span class="sep">&#183;</span>${queryResult.columnCount} columns
                                    <span class="sep">&#183;</span>${queryResult.elapsedMs} ms
                                </span>
                            </div>
                            <c:choose>
                                <c:when test="${queryResult.rowCount == 0}">
                                    <div class="empty">
                                        <h3>The statement ran and matched nothing</h3>
                                        <p>No rows came back. That is a result, not an error.</p>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="table-wrap">
                                        <table>
                                            <thead>
                                            <tr>
                                                <c:forEach var="col" items="${queryResult.columns}" varStatus="s">
                                                    <th class="mono"><c:out value="${col}"/>
                                                        <span class="faint" style="font-weight:400">
                                                            <c:out value="${queryResult.types[s.index]}"/></span></th>
                                                </c:forEach>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            <c:forEach var="row" items="${queryResult.rows}">
                                                <tr>
                                                    <c:forEach var="cell" items="${row}">
                                                        <td class="mono">
                                                            <c:choose>
                                                                <c:when test="${cell == null}">
                                                                    <span class="faint">NULL</span></c:when>
                                                                <c:when test="${cell == '(hidden)'}">
                                                                    <span class="badge badge-warn">hidden</span></c:when>
                                                                <c:otherwise><c:out value="${cell}"/></c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                    </c:forEach>
                                                </tr>
                                            </c:forEach>
                                            </tbody>
                                        </table>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${queryResult.truncated}">
                                <div class="card-foot">
                                    Stopped at ${maxRows} rows. There may be more behind this,
                                    the cap is applied by the driver with
                                    <span class="mono">setMaxRows</span>.
                                </div>
                            </c:if>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </c:if>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
