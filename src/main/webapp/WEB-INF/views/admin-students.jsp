<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Students" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Student register</h1>
        <p class="lede">Search by name, roll number or branch. Open a record to see every
            application, download the resume or suspend the account.</p>
    </div>
    <div class="spacer"></div>
    <a class="btn" href="${ctx}/admin/import">Bulk import</a>
</div>

<div class="card">
    <div class="card-head">
        <form method="get" action="${ctx}/admin/students" style="display:flex; gap:9px; flex:1; max-width:460px; flex-wrap:wrap">
            <input type="search" name="q" placeholder="Name, roll number or branch"
                   value="<c:out value='${query}'/>">
            <button class="btn" type="submit">Search</button>
            <c:if test="${not empty query}">
                <a class="btn" href="${ctx}/admin/students">Clear</a>
            </c:if>
        </form>
        <div class="spacer"></div>
        <span class="badge badge-neutral">${students.size()} shown</span>
    </div>

    <c:choose>
        <c:when test="${empty students}">
            <div class="empty">
                <h3>${empty query ? 'No students registered yet' : 'Nothing matched that search'}</h3>
                <p class="mb-0">
                    <c:choose>
                        <c:when test="${empty query}">Students appear here as they register,
                            or you can <a href="${ctx}/admin/import">import a batch</a>.</c:when>
                        <c:otherwise>Try a shorter search, or
                            <a href="${ctx}/admin/students">list everybody</a>.</c:otherwise>
                    </c:choose>
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Student</th><th>Branch</th><th>CGPA</th><th>Backlogs</th>
                        <th>Applications</th><th>Account</th><th></th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="s" items="${students}">
                        <tr>
                            <td>
                                <a href="${ctx}/admin/student?id=${s.id}">
                                    <strong><c:out value="${s.user.fullName}"/></strong></a>
                                <span class="sub"><c:out value="${s.rollNo}"/>
                                    <span class="sep">&#183;</span><c:out value="${s.user.email}"/></span>
                            </td>
                            <td><c:out value="${s.branch}"/>
                                <span class="sub">batch ${s.batchYear}</span></td>
                            <td class="num">${s.cgpa}</td>
                            <td class="num">${s.backlogs}</td>
                            <td class="num">${empty appCounts[s.id] ? 0 : appCounts[s.id]}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${not s.user.active}">
                                        <span class="badge badge-bad">Suspended</span></c:when>
                                    <c:when test="${s.user.locked}">
                                        <span class="badge badge-warn">Locked</span></c:when>
                                    <c:when test="${placedIds.contains(s.id)}">
                                        <span class="badge badge-ok">Placed</span></c:when>
                                    <c:otherwise><span class="badge badge-neutral">Active</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td class="right">
                                <a class="btn btn-sm" href="${ctx}/admin/student?id=${s.id}">Open</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="layout-bottom.jspf" %>
