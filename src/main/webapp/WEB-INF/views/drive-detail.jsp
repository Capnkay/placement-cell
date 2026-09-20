<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Drive" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1><c:out value="${drive.company.name}"/></h1>
        <p class="lede"><c:out value="${drive.jobRole}"/> <span class="sep">&#183;</span>
            <c:out value="${drive.location}"/> <span class="sep">&#183;</span>
            <span class="num">${drive.packageLpa}</span> LPA</p>
    </div>
    <div class="spacer"></div>
    <a class="btn" href="${ctx}/app/drives">Back to the board</a>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h2>About this drive</h2>
                <div class="spacer"></div>
                <span class="badge ${drive.status.badgeClass}">${drive.status.label}</span>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty drive.description}">
                        <p class="muted">No description was added for this drive.</p>
                    </c:when>
                    <c:otherwise>
                        <p><c:out value="${drive.description}"/></p>
                    </c:otherwise>
                </c:choose>

                <dl class="kv mt-2">
                    <dt>Sector</dt><dd><c:out value="${drive.company.sector}"/></dd>
                    <dt>Drive date</dt><dd>${drive.driveDate}</dd>
                    <dt>Applications close</dt><dd>${drive.lastDate}</dd>
                    <dt>Recruiter contact</dt><dd class="mono"><c:out value="${drive.company.hrEmail}"/></dd>
                    <c:if test="${not empty drive.company.website}">
                        <dt>Website</dt>
                        <dd><a href="<c:out value='${drive.company.website}'/>" rel="noreferrer noopener"
                               target="_blank"><c:out value="${drive.company.website}"/></a></dd>
                    </c:if>
                </dl>
            </div>
        </div>

        <div class="card">
            <div class="card-head"><h2>Eligibility rules</h2></div>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>Requirement</th><th>This drive asks for</th>
                        <th>Your record</th><th class="right">Met</th></tr></thead>
                    <tbody>
                    <tr>
                        <td>Minimum CGPA</td>
                        <td class="num">${drive.minCgpa}</td>
                        <td class="num">${profile.cgpa}</td>
                        <td class="right">
                            <c:choose>
                                <c:when test="${profile.cgpa >= drive.minCgpa}">
                                    <span class="badge badge-ok">Yes</span></c:when>
                                <c:otherwise><span class="badge badge-bad">No</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td>Backlogs allowed</td>
                        <td class="num">${drive.maxBacklogs}</td>
                        <td class="num">${profile.backlogs}</td>
                        <td class="right">
                            <c:choose>
                                <c:when test="${profile.backlogs <= drive.maxBacklogs}">
                                    <span class="badge badge-ok">Yes</span></c:when>
                                <c:otherwise><span class="badge badge-bad">No</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td>Branch</td>
                        <td>
                            <c:forEach var="b" items="${drive.branchList}">
                                <span class="chip"><c:out value="${b}"/></span>
                            </c:forEach>
                        </td>
                        <td><c:out value="${profile.branch}"/></td>
                        <td class="right">
                            <c:choose>
                                <c:when test="${drive.branchList.contains(profile.branch)}">
                                    <span class="badge badge-ok">Yes</span></c:when>
                                <c:otherwise><span class="badge badge-bad">No</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <div>
        <div class="card">
            <div class="card-head"><h3>Your position</h3></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${verdict.alreadyApplied}">
                        <div class="alert alert-info mb-0"><svg class="i"><use href="#i-check"/></svg>
                            <div>You have already applied to this drive.
                                <a href="${ctx}/app/applications">Track it here</a>.</div>
                        </div>
                    </c:when>
                    <c:when test="${verdict.eligible}">
                        <div class="alert alert-ok"><svg class="i"><use href="#i-check"/></svg>
                            <div>You meet every requirement on this drive.</div>
                        </div>
                        <form method="post" action="${ctx}/app/apply">
                            <input type="hidden" name="csrf" value="${csrf}">
                            <input type="hidden" name="driveId" value="${drive.id}">
                            <button class="btn btn-primary" type="submit" style="width:100%; justify-content:center">
                                Apply to this drive
                            </button>
                        </form>
                        <p class="small muted mt-1 mb-0">
                            The placement cell is notified as soon as you apply. You can withdraw
                            while the application is still at the applied stage.
                        </p>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-bad"><svg class="i"><use href="#i-alert"/></svg>
                            <div><strong>You are not eligible for this drive.</strong></div>
                        </div>
                        <ul class="reasons">
                            <c:forEach var="r" items="${verdict.reasons}">
                                <li><c:out value="${r}"/></li>
                            </c:forEach>
                        </ul>
                        <p class="small muted mt-1 mb-0">
                            If your record is out of date, correct it on your
                            <a href="${ctx}/app/profile">profile</a> and this page will
                            recalculate straight away.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
