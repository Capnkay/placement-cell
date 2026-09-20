<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="theme" value="${empty cookie.portal_theme ? 'light' : cookie.portal_theme.value}"/>
<!DOCTYPE html>
<html lang="en" data-theme="${theme}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Not allowed : Placement Cell</title>
    <link rel="stylesheet" href="${ctx}/assets/app.css">
    <link rel="stylesheet" href="${ctx}/assets/app2.css">
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <script src="${ctx}/assets/app.js" defer></script>
</head>
<body>
<main class="page" style="max-width:640px; padding-top:80px">
    <div class="card">
        <div class="card-body" style="text-align:center; padding:44px 30px">
            <h1>That area is for the placement cell</h1>
            <p class="muted">Your account does not have the officer role, so this page is closed
                to it. Nothing has been changed and nobody has been notified.</p>
            <c:if test="${not empty deniedPath}">
                <p class="small mono muted"><c:out value="${deniedPath}"/></p>
            </c:if>
            <div class="btn-row" style="justify-content:center">
                <a class="btn btn-primary" href="${ctx}/app/dashboard">Back to my dashboard</a>
                <a class="btn" href="${ctx}/logout">Sign out</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
