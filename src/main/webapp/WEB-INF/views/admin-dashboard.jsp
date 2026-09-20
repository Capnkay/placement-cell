<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Overview" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Placement overview</h1>
        <p class="lede">Where the season stands across every company, drive and candidate.</p>
    </div>
    <div class="spacer"></div>
    <div class="btn-row">
        <a class="btn" href="${ctx}/admin/import">Import students</a>
        <a class="btn btn-primary" href="${ctx}/admin/drives">
            <svg class="i i-sm"><use href="#i-plus"/></svg> Announce a drive</a>
    </div>
</div>

<div class="metrics">
    <div class="metric">
        <span class="k">Students</span>
        <span class="v">${studentCount}</span>
        <span class="n">on the register</span>
    </div>
    <div class="metric">
        <span class="k">Placed</span>
        <span class="v">${placedCount}</span>
        <span class="n">${placementRate}% of the register</span>
    </div>
    <div class="metric">
        <span class="k">Open drives</span>
        <span class="v">${openDriveCount}</span>
        <span class="n">of ${driveCount} announced</span>
    </div>
    <div class="metric">
        <span class="k">Applications</span>
        <span class="v">${applicationCount}</span>
        <span class="n">from ${companyCount} recruiters</span>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h2>Recent applications</h2>
                <div class="spacer"></div>
                <a class="btn btn-sm" href="${ctx}/admin/applications">Open the pipeline</a>
            </div>
            <c:choose>
                <c:when test="${empty recent}">
                    <div class="empty">
                        <h3>No applications yet</h3>
                        <p>Announce a drive and applications will appear here as students apply.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Candidate</th><th>Drive</th><th>Status</th><th>Applied</th></tr></thead>
                            <tbody>
                            <c:forEach var="a" items="${recent}">
                                <tr>
                                    <td>
                                        <a href="${ctx}/admin/student?id=${a.student.id}" class="strong">
                                            <c:out value="${a.student.user.fullName}"/></a>
                                        <span class="sub"><c:out value="${a.student.rollNo}"/>
                                            <span class="sep">&#183;</span><c:out value="${a.student.branch}"/></span>
                                    </td>
                                    <td><c:out value="${a.drive.company.name}"/>
                                        <span class="sub"><c:out value="${a.drive.jobRole}"/></span></td>
                                    <td><span class="badge ${a.status.badgeClass}">${a.status.label}</span></td>
                                    <td class="nowrap muted">${a.appliedAt.toLocalDate()}</td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <div>
        <div class="card">
            <div class="card-head"><h3>Pipeline</h3></div>
            <div class="card-body">
                <c:forEach var="e" items="${breakdown}">
                    <div class="bar-row">
                        <span class="bar-label">
                            <span class="badge ${e.key.badgeClass}">${e.key.label}</span>
                        </span>
                        <span class="bar"><span style="width:${e.value * 100 / peak}%"></span></span>
                        <span class="bar-value">${e.value}</span>
                    </div>
                </c:forEach>
            </div>
        </div>

        <div class="card">
            <div class="card-head">
                <h3>Latest activity</h3>
                <div class="spacer"></div>
                <a class="small" href="${ctx}/admin/audit">Full trail</a>
            </div>
            <c:choose>
                <c:when test="${empty audit}">
                    <div class="card-body"><p class="muted small mb-0">Nothing recorded yet.</p></div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <tbody>
                            <c:forEach var="row" items="${audit}">
                                <tr>
                                    <td>
                                        <span class="mono"><c:out value="${row.action}"/></span>
                                        <span class="sub">
                                            <c:out value="${row.at.toLocalDate()}"/>
                                            <c:if test="${not empty row.durationMs}">
                                                <span class="sep">&#183;</span>${row.durationMs} ms
                                            </c:if>
                                        </span>
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
</div>

<%@ include file="layout-bottom.jspf" %>
