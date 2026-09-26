<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Student" scope="request"/>
<%@ include file="layout-top.jspf" %>

<nav class="crumbs" aria-label="Breadcrumb">
    <a href="${ctx}/admin/students">Students</a><span aria-hidden="true">/</span>
    <span aria-current="page"><c:out value="${profile.user.fullName}"/></span>
</nav>

<div class="page-head">
    <div>
        <h1><c:out value="${profile.user.fullName}"/></h1>
        <p class="lede"><c:out value="${profile.rollNo}"/> <span class="sep">&#183;</span>
            <c:out value="${profile.branch}"/> <span class="sep">&#183;</span>
            batch ${profile.batchYear} <span class="sep">&#183;</span>
            <span class="mono"><c:out value="${profile.user.email}"/></span></p>
    </div>
    <div class="spacer"></div>
    <a class="btn" href="${ctx}/admin/students">Back to the register</a>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h2>Applications</h2>
                <div class="spacer"></div>
                <span class="badge badge-neutral">${applications.size()}</span>
            </div>
            <c:choose>
                <c:when test="${empty applications}">
                    <div class="empty">
                        <h3>This student has not applied anywhere</h3>
                        <p class="mb-0">Nothing to review yet.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Drive</th><th>Applied</th><th>Status</th><th>Note</th></tr></thead>
                            <tbody>
                            <c:forEach var="a" items="${applications}">
                                <tr>
                                    <td><strong><c:out value="${a.drive.company.name}"/></strong>
                                        <span class="sub"><c:out value="${a.drive.jobRole}"/></span></td>
                                    <td class="nowrap small muted">${a.appliedAt.toLocalDate()}</td>
                                    <td><span class="badge ${a.status.badgeClass}">${a.status.label}</span></td>
                                    <td class="small muted"><c:out value="${empty a.remarks ? '-' : a.remarks}"/></td>
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
            <div class="card-head"><h3>Academic record</h3></div>
            <div class="card-body">
                <dl class="kv">
                    <dt>CGPA</dt><dd class="num">${profile.cgpa}</dd>
                    <dt>Live backlogs</dt><dd class="num">${profile.backlogs}</dd>
                    <dt>Graduating</dt><dd>${profile.batchYear}</dd>
                    <dt>Mobile</dt><dd><c:out value="${empty profile.phone ? 'not given' : profile.phone}"/></dd>
                    <dt>Resume</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${empty profile.resumeFile}">
                                <span class="badge badge-warn">Not uploaded</span></c:when>
                            <c:otherwise>
                                <a href="${ctx}/app/resume?student=${profile.id}">Download</a></c:otherwise>
                        </c:choose>
                    </dd>
                </dl>
            </div>
        </div>

        <div class="card">
            <div class="card-head"><h3>Account</h3></div>
            <div class="card-body">
                <dl class="kv">
                    <dt>Status</dt>
                    <dd>
                        <c:choose>
                            <c:when test="${not profile.user.active}">
                                <span class="badge badge-bad">Suspended</span></c:when>
                            <c:when test="${profile.user.locked}">
                                <span class="badge badge-warn">Locked out</span></c:when>
                            <c:otherwise><span class="badge badge-ok">Active</span></c:otherwise>
                        </c:choose>
                    </dd>
                    <dt>Failed attempts</dt><dd class="num">${profile.user.failedAttempts}</dd>
                    <dt>Last sign in</dt>
                    <dd><c:out value="${empty profile.user.lastLogin ? 'never' : profile.user.lastLogin.toLocalDate()}"/></dd>
                    <dt>Registered</dt><dd>${profile.user.createdAt.toLocalDate()}</dd>
                </dl>

                <div class="btn-row mt-2">
                    <c:choose>
                        <c:when test="${profile.user.active}">
                            <form class="inline-form" method="post" action="${ctx}/admin/student"
                                  data-confirm="Stop <c:out value='${profile.user.fullName}'/> from signing in?">
                                <input type="hidden" name="csrf" value="${csrf}">
                                <input type="hidden" name="id" value="${profile.id}">
                                <input type="hidden" name="action" value="suspend">
                                <button class="btn btn-sm" type="submit">Suspend account</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form class="inline-form" method="post" action="${ctx}/admin/student">
                                <input type="hidden" name="csrf" value="${csrf}">
                                <input type="hidden" name="id" value="${profile.id}">
                                <input type="hidden" name="action" value="restore">
                                <button class="btn btn-sm btn-primary" type="submit">Restore account</button>
                            </form>
                        </c:otherwise>
                    </c:choose>

                    <c:if test="${profile.user.locked}">
                        <form class="inline-form" method="post" action="${ctx}/admin/student">
                            <input type="hidden" name="csrf" value="${csrf}">
                            <input type="hidden" name="id" value="${profile.id}">
                            <input type="hidden" name="action" value="unlock">
                            <button class="btn btn-sm" type="submit">Clear the lockout</button>
                        </form>
                    </c:if>
                </div>

                <form class="mt-2" method="post" action="${ctx}/admin/student"
                      data-confirm="Remove <c:out value='${profile.user.fullName}'/> and every application? This cannot be undone.">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="id" value="${profile.id}">
                    <input type="hidden" name="action" value="delete">
                    <button class="btn btn-sm btn-danger" type="submit">Remove from the register</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
