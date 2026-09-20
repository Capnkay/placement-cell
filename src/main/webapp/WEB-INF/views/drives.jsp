<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Drives" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Placement drives</h1>
        <p class="lede">Your record is checked against every rule on each drive. Where you are not
            eligible, the exact reason is shown instead of the Apply button.</p>
    </div>
    <div class="spacer"></div>
    <p class="muted small mb-0" style="padding-top:6px">
        <span class="strong num">${eligibleCount}</span> eligible of
        <span class="num">${drives.size()}</span> shown
    </p>
</div>

<div class="tabs">
    <a href="${ctx}/app/drives" class="${filter == 'open' ? 'active' : ''}">Open drives</a>
    <a href="${ctx}/app/drives?show=all" class="${filter == 'all' ? 'active' : ''}">Everything on the board</a>
</div>

<c:choose>
    <c:when test="${empty drives}">
        <div class="card"><div class="empty">
            <h3>No drives on the board</h3>
            <p class="mb-0">The placement cell has not announced anything yet.</p>
        </div></div>
    </c:when>
    <c:otherwise>
        <div class="grid cols-2">
            <c:forEach var="d" items="${drives}">
                <c:set var="v" value="${verdicts[d.id]}"/>
                <div class="card">
                    <div class="card-head">
                        <div>
                            <h3 class="mb-0"><c:out value="${d.company.name}"/></h3>
                            <span class="small muted"><c:out value="${d.jobRole}"/> <span class="sep">&#183;</span>
                                <c:out value="${d.location}"/></span>
                        </div>
                        <div class="spacer"></div>
                        <span class="badge ${d.status.badgeClass}">${d.status.label}</span>
                    </div>
                    <div class="card-body">
                        <div class="grid cols-3" style="gap:10px">
                            <div>
                                <div class="label small muted">Package</div>
                                <strong class="num">${d.packageLpa} LPA</strong>
                            </div>
                            <div>
                                <div class="label small muted">Minimum CGPA</div>
                                <strong class="num">${d.minCgpa}</strong>
                            </div>
                            <div>
                                <div class="label small muted">Backlogs allowed</div>
                                <strong class="num">${d.maxBacklogs}</strong>
                            </div>
                        </div>

                        <div class="mt-1">
                            <c:forEach var="b" items="${d.branchList}">
                                <span class="chip"><c:out value="${b}"/></span>
                            </c:forEach>
                        </div>

                        <p class="small muted mt-1 mb-0">
                            Drive on ${d.driveDate} <span class="sep">&#183;</span>
                            applications close ${d.lastDate}
                        </p>

                        <div class="mt-2">
                            <c:choose>
                                <c:when test="${v.alreadyApplied}">
                                    <div class="btn-row">
                                        <span class="badge badge-brand">Already applied</span>
                                        <a class="btn btn-sm" href="${ctx}/app/applications">Track it</a>
                                    </div>
                                </c:when>
                                <c:when test="${v.eligible}">
                                    <div class="btn-row">
                                        <a class="btn btn-sm btn-primary" href="${ctx}/app/drive?id=${d.id}">
                                            View and apply
                                        </a>
                                        <span class="small muted">You meet every requirement</span>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-bad">Not eligible</span>
                                    <ul class="reasons">
                                        <c:forEach var="r" items="${v.reasons}">
                                            <li><c:out value="${r}"/></li>
                                        </c:forEach>
                                    </ul>
                                    <a class="small" href="${ctx}/app/drive?id=${d.id}">See the full posting</a>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="layout-bottom.jspf" %>
