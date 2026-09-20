<%@ page contentType="text/html; charset=UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="theme" value="${empty cookie.portal_theme ? 'light' : cookie.portal_theme.value}"/>
<!DOCTYPE html>
<html lang="en" data-theme="${theme}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Something went wrong : Placement Cell</title>
    <link rel="stylesheet" href="${ctx}/assets/app.css">
    <link rel="stylesheet" href="${ctx}/assets/app2.css">
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <script src="${ctx}/assets/app.js" defer></script>
</head>
<body>
<main class="page" style="max-width:700px; padding-top:70px">
    <div class="card">
        <div class="card-body" style="padding:40px 34px">
            <h1>The server could not finish that request</h1>
            <p class="muted">Nothing was half saved: the transaction around the failed call
                was rolled back, so the database is in the state it was in before.</p>

            <%-- The class name alone is shown. A stack trace on a page tells an attacker
                 which libraries are in play, so the detail stays in the server log. --%>
            <c:if test="${not empty pageContext.exception}">
                <p class="small mono muted">
                    <c:out value="${pageContext.exception['class'].simpleName}"/>
                </p>
            </c:if>

            <div class="btn-row">
                <a class="btn btn-primary" href="${ctx}/">Back to the portal</a>
                <a class="btn" href="${ctx}/logout">Sign out</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
