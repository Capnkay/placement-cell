<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
    The welcome file. It holds no markup at all: it decides where the visitor
    belongs and sends them there with the JSTL c:redirect tag. That is a redirect,
    not a forward, so the browser is told to ask for the new address.
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
