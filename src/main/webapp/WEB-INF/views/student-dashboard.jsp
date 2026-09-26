<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Overview" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1><c:out value="${me.fullName}"/></h1>
        <p class="lede"><c:out value="${profile.rollNo}"/>
            <span class="sep">&#183;</span><c:out value="${profile.branch}"/>
            <span class="sep">&#183;</span>graduating ${profile.batchYear}
            <span class="sep">&#183;</span>CGPA <span class="num">${profile.cgpa}</span></p>
    </div>
    <div class="spacer"></div>
    <a class="btn btn-primary" href="${ctx}/app/drives">
        Browse drives <svg class="i i-sm"><use href="#i-arrow-right"/></svg>
    </a>
</div>

<c:if test="${placed}">
    <div class="alert alert-ok">
        <svg class="i"><use href="#i-check"/></svg>
        <div><strong>You have an offer.</strong> Your other applications stay on the board,
            and the placement cell will be in touch about the policy on further drives.</div>
    </div>
</c:if>

<%-- A short guide for someone new. It disappears once the profile is complete and
     there is at least one application, so it never nags an established user. --%>
<c:set var="hasApplied" value="${not empty applications}"/>
<c:if test="${not profileComplete or not hasApplied}">
    <c:set var="doneCount" value="${1 + (profileComplete ? 1 : 0) + (hasApplied ? 1 : 0)}"/>
    <section class="card checklist" aria-labelledby="checklistTitle">
        <div class="checklist-head">
            <div>
                <h2 id="checklistTitle">${doneCount == 1 ? 'Welcome. Here is how to get started' : 'You are getting there'}</h2>
                <p class="muted mb-0">${doneCount} of 3 steps done</p>
            </div>
            <div class="spacer"></div>
            <div class="checklist-bar" role="progressbar" aria-valuemin="0" aria-valuemax="3"
                 aria-valuenow="${doneCount}"><span class="w${doneCount}"></span></div>
        </div>
        <ol class="checklist-steps">
            <li class="done">
                <span class="tick"><svg class="i"><use href="#i-check"/></svg></span>
                <div><strong>Create your account</strong>
                    <span>Done. Your academic record decides which drives you see.</span></div>
            </li>
            <li class="${profileComplete ? 'done' : 'next'}">
                <span class="tick"><c:choose>
                    <c:when test="${profileComplete}"><svg class="i"><use href="#i-check"/></svg></c:when>
                    <c:otherwise>2</c:otherwise></c:choose></span>
                <div><strong>Finish your profile</strong>
                    <span>${profileComplete ? 'Resume and phone number are on file.' : 'Add a phone number and upload your resume. Recruiters ask for both.'}</span></div>
                <c:if test="${not profileComplete}">
                    <a class="btn btn-sm btn-primary" href="${ctx}/app/profile">Open profile</a>
                </c:if>
            </li>
            <li class="${hasApplied ? 'done' : (profileComplete ? 'next' : '')}">
                <span class="tick"><c:choose>
                    <c:when test="${hasApplied}"><svg class="i"><use href="#i-check"/></svg></c:when>
                    <c:otherwise>3</c:otherwise></c:choose></span>
                <div><strong>Apply to your first drive</strong>
                    <span><c:choose>
                        <c:when test="${hasApplied}">You have applied. Follow it under Applications.</c:when>
                        <c:when test="${eligibleOpen.size() == 0}">No open drive matches your record right now.</c:when>
                        <c:otherwise>${eligibleOpen.size()} open drive(s) match your record.</c:otherwise>
                    </c:choose></span></div>
                <c:if test="${not hasApplied}">
                    <a class="btn btn-sm ${profileComplete ? 'btn-primary' : ''}" href="${ctx}/app/drives">Browse drives</a>
                </c:if>
            </li>
        </ol>
    </section>
</c:if>

<div class="metrics">
    <div class="metric">
        <span class="k">Applications</span>
        <span class="v">${applications.size()}</span>
        <span class="n">across all drives</span>
    </div>
    <div class="metric">
        <span class="k">Eligible now</span>
        <span class="v">${eligibleOpen.size()}</span>
        <span class="n">of ${openCount} open</span>
    </div>
    <div class="metric">
        <span class="k">Live backlogs</span>
        <span class="v">${profile.backlogs}</span>
        <span class="n">${profile.backlogs == 0 ? 'clear record' : 'limits some drives'}</span>
    </div>
    <div class="metric">
        <span class="k">Resume</span>
        <span class="v" style="font-size:15px; padding-top:3px">
            <c:choose>
                <c:when test="${empty profile.resumeFile}">
                    <span class="badge badge-warn">Not uploaded</span></c:when>
                <c:otherwise><span class="badge badge-ok">On file</span></c:otherwise>
            </c:choose>
        </span>
        <span class="n"><a href="${ctx}/app/profile">manage</a></span>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h2>Drives you can apply to</h2>
                <div class="spacer"></div>
                <a class="btn btn-sm" href="${ctx}/app/drives">See all</a>
            </div>
            <c:choose>
                <c:when test="${empty eligibleOpen}">
                    <div class="empty">
                        <h3>Nothing open for you right now</h3>
                        <p>Either you have applied to every open drive, or your record does not
                            meet their rules yet. The
                            <a href="${ctx}/app/drives?show=all">full board</a> explains each one.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead>
                            <tr><th>Company</th><th>Role</th><th class="right">Package</th>
                                <th>Closes</th><th></th></tr>
                            </thead>
                            <tbody>
                            <c:forEach var="d" items="${eligibleOpen}">
                                <tr>
                                    <td><span class="strong"><c:out value="${d.company.name}"/></span>
                                        <span class="sub"><c:out value="${d.company.sector}"/></span></td>
                                    <td><c:out value="${d.jobRole}"/>
                                        <span class="sub"><c:out value="${d.location}"/></span></td>
                                    <td class="right num">${d.packageLpa} LPA</td>
                                    <td class="nowrap muted">${d.lastDate}</td>
                                    <td class="right">
                                        <a class="btn btn-sm" href="${ctx}/app/drive?id=${d.id}">View</a>
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
                <h2>Recent applications</h2>
                <div class="spacer"></div>
                <a class="btn btn-sm" href="${ctx}/app/applications">All applications</a>
            </div>
            <c:choose>
                <c:when test="${empty recentApplications}">
                    <div class="empty">
                        <h3>You have not applied anywhere yet</h3>
                        <p>Applications you make will be listed here with their current stage.
                            Start with the <a href="${ctx}/app/drives">open drives</a>.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Company</th><th>Role</th><th>Status</th><th>Applied</th></tr></thead>
                            <tbody>
                            <c:forEach var="a" items="${recentApplications}">
                                <tr>
                                    <td class="strong"><c:out value="${a.drive.company.name}"/></td>
                                    <td><c:out value="${a.drive.jobRole}"/></td>
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
            <div class="card-head"><h3>Where your applications stand</h3></div>
            <div class="card-body">
                <c:forEach var="e" items="${statusCounts}">
                    <div class="bar-row">
                        <span class="bar-label">
                            <span class="badge ${e.key.badgeClass}">${e.key.label}</span>
                        </span>
                        <span class="bar">
                            <span style="width:${applications.size() == 0 ? 0 : (e.value * 100 / applications.size())}%"></span>
                        </span>
                        <span class="bar-value">${e.value}</span>
                    </div>
                </c:forEach>
            </div>
            <div class="card-foot">The placement cell updates these as each round is decided.</div>
        </div>

        <div class="card">
            <div class="card-head">
                <svg class="i icon-chip"><use href="#i-user"/></svg>
                <h3>Your record</h3>
            </div>
            <div class="card-body">
                <dl class="kv">
                    <dt>Roll number</dt><dd><c:out value="${profile.rollNo}"/></dd>
                    <dt>Branch</dt><dd><c:out value="${profile.branch}"/></dd>
                    <dt>Graduating</dt><dd>${profile.batchYear}</dd>
                    <dt>CGPA</dt><dd class="num">${profile.cgpa}</dd>
                    <dt>Backlogs</dt><dd class="num">${profile.backlogs}</dd>
                    <dt>Mobile</dt>
                    <dd><c:out value="${empty profile.phone ? 'not given' : profile.phone}"/></dd>
                </dl>
                <div class="btn-row mt-2">
                    <a class="btn btn-sm" href="${ctx}/app/profile">Edit profile</a>
                    <c:if test="${not empty profile.resumeFile}">
                        <a class="btn btn-sm" href="${ctx}/app/resume">
                            <svg class="i i-sm"><use href="#i-download"/></svg> Resume</a>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
