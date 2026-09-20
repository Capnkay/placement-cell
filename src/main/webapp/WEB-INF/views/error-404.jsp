<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="theme" value="${empty cookie.portal_theme ? 'light' : cookie.portal_theme.value}"/>
<!DOCTYPE html>
<html lang="en" data-theme="${theme}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Page not found : Placement Cell</title>
    <link rel="stylesheet" href="${ctx}/assets/app.css">
    <link rel="stylesheet" href="${ctx}/assets/app2.css">
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <script src="${ctx}/assets/app.js" defer></script>
</head>
<body>
<main class="page" style="max-width:640px; padding-top:80px">
    <div class="card">
        <div class="card-body" style="text-align:center; padding:44px 30px">
            <h1>There is nothing at that address</h1>
            <p class="muted">The link may be old, or a drive that used to be here has been
                removed from the board.</p>
            <div class="btn-row" style="justify-content:center">
                <a class="btn btn-primary" href="${ctx}/">Go to the portal</a>
            </div>
        </div>
    </div>
</main>
</body>
</html>
