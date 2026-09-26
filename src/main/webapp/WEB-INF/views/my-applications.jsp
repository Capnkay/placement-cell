<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="My applications" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>My applications</h1>
        <p class="lede">Every drive you have applied to, with the placement cell's latest note.</p>
    </div>
    <div class="spacer"></div>
    <a class="btn btn-primary" href="${ctx}/app/drives">Find more drives</a>
</div>

<c:choose>
    <c:when test="${empty applications}">
        <div class="card"><div class="empty">
            <h3>Nothing here yet</h3>
            <p class="mb-0">When you apply to a drive it appears here, and you can follow it
                from shortlist through to the result. <a href="${ctx}/app/drives">Browse the board</a>.</p>
        </div></div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Company</th><th>Role</th><th>Drive date</th>
                        <th>Status</th><th>Note from the cell</th><th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="a" items="${applications}">
                        <tr>
                            <td>
                                <strong><c:out value="${a.drive.company.name}"/></strong>
                                <span class="sub">applied <c:out value="${a.appliedAt.toLocalDate()}"/></span>
                            </td>
                            <td><c:out value="${a.drive.jobRole}"/>
                                <span class="sub"><c:out value="${a.drive.location}"/></span></td>
                            <td class="nowrap">${a.drive.driveDate}</td>
                            <td><span class="badge ${a.status.badgeClass}">${a.status.label}</span></td>
                            <td class="small muted">
                                <c:out value="${empty a.remarks ? 'No note yet' : a.remarks}"/>
                            </td>
                            <td class="right">
                                <c:choose>
                                    <c:when test="${a.status == 'APPLIED'}">
                                        <form class="inline-form" method="post" action="${ctx}/app/withdraw"
                                              data-confirm="Withdraw your application to <c:out value='${a.drive.company.name}'/>?">
                                            <input type="hidden" name="csrf" value="${csrf}">
                                            <input type="hidden" name="applicationId" value="${a.id}">
                                            <button class="btn btn-sm btn-danger" type="submit">Withdraw</button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="small muted nowrap">In progress with the cell</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
        <p class="small muted mt-1">
            An application can only be withdrawn while it is still at the applied stage.
            Once the placement cell shortlists you, withdrawing is a conversation with the office.
        </p>
    </c:otherwise>
</c:choose>

<%@ include file="layout-bottom.jspf" %>
