<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Shortlist" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Batch shortlisting</h1>
        <p class="lede">Tick candidates as you read through them, then commit the whole set in one go.
            Your picks stick around across page loads until you commit them or close the
            session, so there is no rush working down a long list.</p>
    </div>
</div>

<div class="card">
    <div class="card-head">
        <form method="get" action="${ctx}/admin/shortlist" style="display:flex; gap:9px; flex:1; max-width:520px; flex-wrap:wrap">
            <select name="drive" style="flex:1">
                <option value="">Choose a drive to work on</option>
                <c:forEach var="d" items="${drives}">
                    <option value="${d.id}" ${drive.id eq d.id ? 'selected' : ''}>
                        <c:out value="${d.company.name}"/> : <c:out value="${d.jobRole}"/>
                    </option>
                </c:forEach>
            </select>
            <button class="btn" type="submit">Open</button>
        </form>
    </div>

    <c:choose>
        <c:when test="${empty drive}">
            <div class="empty">
                <h3>Pick a drive to start</h3>
                <p class="mb-0">The applicant list appears here with a tick box beside each name.</p>
            </div>
        </c:when>
        <c:when test="${empty applications}">
            <div class="empty">
                <h3>Nobody has applied to this drive</h3>
                <p class="mb-0">There is nothing to shortlist yet.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card-body tight" style="display:flex; align-items:center; gap:14px; flex-wrap:wrap">
                <strong><c:out value="${drive.company.name}"/> : <c:out value="${drive.jobRole}"/></strong>
                <span class="badge badge-brand">${pickedCount} selected</span>
                <div class="spacer" style="margin-left:auto"></div>
                <form class="inline-form" method="post" action="${ctx}/admin/shortlist">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="driveId" value="${drive.id}">
                    <input type="hidden" name="action" value="clear">
                    <button class="btn btn-sm" type="submit" ${pickedCount == 0 ? 'disabled' : ''}>
                        Clear selection</button>
                </form>
                <form class="inline-form" method="post" action="${ctx}/admin/shortlist">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="driveId" value="${drive.id}">
                    <input type="hidden" name="action" value="commit">
                    <button class="btn btn-sm btn-primary" type="submit" ${pickedCount == 0 ? 'disabled' : ''}>
                        Shortlist the ${pickedCount} selected</button>
                </form>
                <form class="inline-form" method="post" action="${ctx}/admin/shortlist">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="action" value="finish">
                    <button class="btn btn-sm" type="submit" title="Closes this shortlisting session">
                        End the session</button>
                </form>
            </div>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th style="width:60px">Pick</th><th>Candidate</th><th>Branch</th>
                        <th>CGPA</th><th>Backlogs</th><th>Status</th><th>Resume</th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${applications}">
                        <tr>
                            <td>
                                <form method="post" action="${ctx}/admin/shortlist">
                                    <input type="hidden" name="csrf" value="${csrf}">
                                    <input type="hidden" name="driveId" value="${drive.id}">
                                    <input type="hidden" name="applicationId" value="${a.id}">
                                    <input type="hidden" name="action" value="toggle">
                                    <button class="btn btn-sm ${picked.contains(a.id) ? 'btn-primary' : ''}"
                                            type="submit" title="Add or remove this candidate">
                                        <c:choose><c:when test="${picked.contains(a.id)}"><svg class="i i-sm"><use href="#i-check"/></svg></c:when><c:otherwise><svg class="i i-sm"><use href="#i-plus"/></svg></c:otherwise></c:choose>
                                    </button>
                                </form>
                            </td>
                            <td>
                                <a href="${ctx}/admin/student?id=${a.student.id}">
                                    <strong><c:out value="${a.student.user.fullName}"/></strong></a>
                                <span class="sub"><c:out value="${a.student.rollNo}"/></span>
                            </td>
                            <td class="small"><c:out value="${a.student.branch}"/></td>
                            <td class="num">${a.student.cgpa}</td>
                            <td class="num">${a.student.backlogs}</td>
                            <td><span class="badge ${a.status.badgeClass}">${a.status.label}</span></td>
                            <td>
                                <c:choose>
                                    <c:when test="${empty a.student.resumeFile}">
                                        <span class="small muted">none</span></c:when>
                                    <c:otherwise>
                                        <a class="small" href="${ctx}/app/resume?student=${a.student.id}">download</a>
                                    </c:otherwise>
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

<div class="card">
    <div class="card-head"><h3>What is happening underneath</h3></div>
    <div class="card-body">
        <p class="small muted mb-0">
            Ticking a name does not touch the database. The identifier goes into a set held
            by a stateful session bean that belongs to your browser session, reached through
            a JNDI lookup rather than an injected field, because a servlet field would be
            shared by every officer at once. Committing writes the whole set in one
            transaction and clears the basket. Ending the session calls the bean's
            <span class="mono">@Remove</span> method, which is the client telling the
            container it is finished with the conversation.
        </p>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
