<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    The welcome file. It holds no markup at all: it decides where the visitor
    belongs and forwards there. jsp:forward is the action element that hands the
    request on inside the server, so the browser never sees this page.
--%>
<c:choose>
    <c:when test="${empty sessionScope.user}">
        <c:redirect url="/login"/>
    </c:when>
    <c:when test="${sessionScope.user.admin}">
        <c:redirect url="/admin/dashboard"/>
    </c:when>
    <c:otherwise>
        <c:redirect url="/app/dashboard"/>
    </c:otherwise>
</c:choose>
