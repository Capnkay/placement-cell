<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Audit" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Audit trail</h1>
        <p class="lede">Not one of these rows is written by a servlet. Each came from the interceptor
            wrapped around a business method, or from the message driven bean that drains
            the notification queue.</p>
    </div>
    <div class="spacer"></div>
    <p class="muted small mb-0" style="padding-top:6px">
        <span class="strong num">${total}</span> rows recorded since first deploy
    </p>
</div>

<div class="card">
    <div class="card-head">
        <form method="get" action="${ctx}/admin/audit" style="display:flex; gap:9px; flex:1; max-width:520px">
            <input type="search" name="q" placeholder="Method, actor or detail"
                   value="<c:out value='${query}'/>">
            <select name="limit" style="width:110px">
                <option value="50" ${limit == 50 ? 'selected' : ''}>50 rows</option>
                <option value="100" ${limit == 100 ? 'selected' : ''}>100 rows</option>
                <option value="250" ${limit == 250 ? 'selected' : ''}>250 rows</option>
                <option value="500" ${limit == 500 ? 'selected' : ''}>500 rows</option>
            </select>
            <button class="btn" type="submit">Search</button>
            <c:if test="${not empty query}"><a class="btn" href="${ctx}/admin/audit">Clear</a></c:if>
        </form>
        <div class="spacer"></div>
        <span class="badge badge-neutral">${entries.size()} shown</span>
    </div>

    <c:choose>
        <c:when test="${empty entries}">
            <div class="empty">
                <h3>${empty query ? 'Nothing recorded yet' : 'Nothing matched that search'}</h3>
                <p class="mb-0">Business method calls appear here as they happen.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Detail</th><th>Took</th></tr></thead>
                    <tbody>
                    <c:forEach var="row" items="${entries}">
                        <tr>
                            <td class="small nowrap muted"><c:out value="${row.at}"/></td>
                            <td class="small"><c:out value="${row.actor}"/></td>
                            <td class="mono small"><c:out value="${row.action}"/></td>
                            <td class="small muted"><c:out value="${row.detail}"/></td>
                            <td class="num small">
                                <c:choose>
                                    <c:when test="${empty row.durationMs}">-</c:when>
                                    <c:otherwise>${row.durationMs} ms</c:otherwise>
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

<%@ include file="layout-bottom.jspf" %>
