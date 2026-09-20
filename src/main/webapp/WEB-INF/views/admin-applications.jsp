<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Pipeline" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Application pipeline</h1>
        <p class="lede">Move a candidate along and the student sees the new status with your note
            straight away. Every change is written to the audit trail too.</p>
    </div>
</div>

<div class="card">
    <div class="card-head">
        <form method="get" action="${ctx}/admin/applications" style="display:flex; gap:9px; flex-wrap:wrap">
            <select name="drive" style="min-width:220px">
                <option value="">Every drive</option>
                <c:forEach var="d" items="${drives}">
                    <option value="${d.id}" ${selectedDrive eq d.id ? 'selected' : ''}>
                        <c:out value="${d.company.name}"/> : <c:out value="${d.jobRole}"/>
                    </option>
                </c:forEach>
            </select>
            <select name="status" style="min-width:170px">
                <option value="">Every status</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s}" ${selectedStatus == s.name() ? 'selected' : ''}>${s.label}</option>
                </c:forEach>
            </select>
            <button class="btn" type="submit">Filter</button>
            <c:if test="${not empty selectedDrive or not empty selectedStatus}">
                <a class="btn" href="${ctx}/admin/applications">Clear</a>
            </c:if>
        </form>
        <div class="spacer"></div>
        <span class="badge badge-neutral">${applications.size()} shown</span>
    </div>

    <c:choose>
        <c:when test="${empty applications}">
            <div class="empty">
                <h3>Nothing matches that filter</h3>
                <p class="mb-0">Widen the filter, or
                    <a href="${ctx}/admin/applications">show every application</a>.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Candidate</th><th>Drive</th><th>Record</th>
                        <th>Status</th><th style="min-width:330px">Move to</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${applications}">
                        <tr>
                            <td>
                                <a href="${ctx}/admin/student?id=${a.student.id}">
                                    <strong><c:out value="${a.student.user.fullName}"/></strong></a>
                                <span class="sub"><c:out value="${a.student.rollNo}"/>
                                    <c:if test="${not empty a.student.resumeFile}">
                                        <span class="sep">&#183;</span>
                                        <a href="${ctx}/app/resume?student=${a.student.id}">resume</a>
                                    </c:if>
                                </span>
                            </td>
                            <td><c:out value="${a.drive.company.name}"/>
                                <span class="sub"><c:out value="${a.drive.jobRole}"/></span></td>
                            <td class="small muted nowrap">
                                CGPA <span class="num">${a.student.cgpa}</span>
                                <span class="sub"><span class="num">${a.student.backlogs}</span> backlog(s)</span>
                            </td>
                            <td><span class="badge ${a.status.badgeClass}">${a.status.label}</span></td>
                            <td>
                                <form method="post" action="${ctx}/admin/applications"
                                      style="display:flex; gap:7px; flex-wrap:wrap">
                                    <input type="hidden" name="csrf" value="${csrf}">
                                    <input type="hidden" name="applicationId" value="${a.id}">
                                    <input type="hidden" name="back" value="/admin/applications">
                                    <select name="status" style="width:150px">
                                        <c:forEach var="s" items="${statuses}">
                                            <option value="${s}" ${a.status == s ? 'selected' : ''}>${s.label}</option>
                                        </c:forEach>
                                    </select>
                                    <input type="text" name="remarks" maxlength="500" style="flex:1; min-width:120px"
                                           placeholder="Note for the student"
                                           value="<c:out value='${a.remarks}'/>">
                                    <button class="btn btn-sm btn-primary" type="submit">Save</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<p class="small muted mt-1">
    To move several candidates at once, use the
    <a href="${ctx}/admin/shortlist">batch shortlist</a> instead — it remembers every pick
    as you work through the list, even across page loads.
</p>

<%@ include file="layout-bottom.jspf" %>
