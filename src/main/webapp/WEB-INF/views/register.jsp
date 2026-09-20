<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="theme" value="${empty cookie.portal_theme ? 'light' : cookie.portal_theme.value}"/>
<!DOCTYPE html>
<html lang="en" data-theme="${theme}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create an account : Placement Cell</title>
    <link rel="stylesheet" href="${ctx}/assets/app.css">
    <link rel="stylesheet" href="${ctx}/assets/app2.css">
    <link rel="icon" href="${ctx}/assets/favicon.svg" type="image/svg+xml">
    <script src="${ctx}/assets/app.js" defer></script>
</head>
<body>
<%@ include file="icons.jspf" %>
<div class="auth">

    <aside class="auth-aside">
        <div class="wrap">
            <a class="brand" href="${ctx}/login" style="margin-bottom:26px">
                <span class="brand-mark"><svg viewBox="0 0 24 24" aria-hidden="true"><use href="#logo"/></svg></span>
                <span>Placement Cell</span>
            </a>
            <h1>Register with the placement cell</h1>
            <p class="lede">Your academic record decides which drives you can apply to, so
                enter it carefully. You can correct any of it later from your profile.</p>
            <ul class="auth-points">
                <li><svg class="i"><use href="#i-user"/></svg>
                    <span><strong>Your college email</strong>
                        This is the address you will sign in with.</span></li>
                <li><svg class="i"><use href="#i-file"/></svg>
                    <span><strong>Your academic record</strong>
                        CGPA, backlogs and a resume, which recruiters read.</span></li>
                <li><svg class="i"><use href="#i-briefcase"/></svg>
                    <span><strong>Then apply</strong>
                        Every drive your record qualifies you for.</span></li>
            </ul>
        </div>
    </aside>

    <main class="auth-main">
        <div class="auth-box wide">
            <h2>Create your account</h2>
            <p class="lede">Every field is checked again on the server before anything is saved.</p>

            <c:if test="${not empty errors['form']}">
                <div class="alert alert-bad"><svg class="i"><use href="#i-alert"/></svg><div><c:out value="${errors['form']}"/></div></div>
            </c:if>
            <c:if test="${not empty errors and empty errors['form']}">
                <div class="alert alert-bad"><svg class="i"><use href="#i-alert"/></svg>
                    <div>Some details need fixing. The fields below explain what.</div>
                </div>
            </c:if>

            <form method="post" action="${ctx}/register" novalidate>
                <input type="hidden" name="csrf" value="${csrf}">

                <div class="field">
                    <label for="fullName">Full name</label>
                    <input type="text" id="fullName" name="fullName" maxlength="120" required
                           value="<c:out value='${form.fullName}'/>"
                           aria-invalid="${not empty errors.fullName ? 'true' : 'false'}">
                    <c:if test="${not empty errors.fullName}"><div class="err"><c:out value="${errors.fullName}"/></div></c:if>
                </div>

                <div class="field">
                    <label for="email">College email address</label>
                    <input type="email" id="email" name="email" maxlength="190" required
                           placeholder="name@campus.edu"
                           value="<c:out value='${form.email}'/>"
                           aria-invalid="${not empty errors.email ? 'true' : 'false'}">
                    <c:choose>
                        <c:when test="${not empty errors.email}"><div class="err"><c:out value="${errors.email}"/></div></c:when>
                        <c:otherwise><div class="help">This is the address you will sign in with.</div></c:otherwise>
                    </c:choose>
                </div>

                <div class="field-row">
                    <div class="field">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password" minlength="8" maxlength="128" required
                               aria-invalid="${not empty errors.password ? 'true' : 'false'}">
                        <c:choose>
                            <c:when test="${not empty errors.password}"><div class="err"><c:out value="${errors.password}"/></div></c:when>
                            <c:otherwise><div class="help">Eight characters or more, letters and digits.</div></c:otherwise>
                        </c:choose>
                    </div>
                    <div class="field">
                        <label for="confirm">Repeat password</label>
                        <input type="password" id="confirm" name="confirm" minlength="8" maxlength="128" required
                               aria-invalid="${not empty errors.confirm ? 'true' : 'false'}">
                        <c:if test="${not empty errors.confirm}"><div class="err"><c:out value="${errors.confirm}"/></div></c:if>
                    </div>
                </div>

                <fieldset>
                    <legend>Academic record</legend>

                    <div class="field-row">
                        <div class="field">
                            <label for="rollNo">Roll number</label>
                            <input type="text" id="rollNo" name="rollNo" maxlength="30" required
                                   value="<c:out value='${form.rollNo}'/>"
                                   aria-invalid="${not empty errors.rollNo ? 'true' : 'false'}">
                            <c:if test="${not empty errors.rollNo}"><div class="err"><c:out value="${errors.rollNo}"/></div></c:if>
                        </div>
                        <div class="field">
                            <label for="branch">Branch</label>
                            <select id="branch" name="branch" required
                                    aria-invalid="${not empty errors.branch ? 'true' : 'false'}">
                                <option value="">Choose your branch</option>
                                <c:forEach var="b" items="${branches}">
                                    <option value="<c:out value='${b}'/>" ${form.branch == b ? 'selected' : ''}>
                                        <c:out value="${b}"/>
                                    </option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty errors.branch}"><div class="err"><c:out value="${errors.branch}"/></div></c:if>
                        </div>
                    </div>

                    <div class="field-row">
                        <div class="field">
                            <label for="batchYear">Graduating year</label>
                            <input type="number" id="batchYear" name="batchYear" required
                                   min="${currentYear - 4}" max="${currentYear + 6}"
                                   value="<c:out value='${empty form.batchYear ? currentYear : form.batchYear}'/>"
                                   aria-invalid="${not empty errors.batchYear ? 'true' : 'false'}">
                            <c:if test="${not empty errors.batchYear}"><div class="err"><c:out value="${errors.batchYear}"/></div></c:if>
                        </div>
                        <div class="field">
                            <label for="cgpa">CGPA</label>
                            <input type="number" id="cgpa" name="cgpa" step="0.01" min="0" max="10" required
                                   value="<c:out value='${form.cgpa}'/>"
                                   aria-invalid="${not empty errors.cgpa ? 'true' : 'false'}">
                            <c:if test="${not empty errors.cgpa}"><div class="err"><c:out value="${errors.cgpa}"/></div></c:if>
                        </div>
                        <div class="field">
                            <label for="backlogs">Live backlogs</label>
                            <input type="number" id="backlogs" name="backlogs" min="0" max="30" required
                                   value="<c:out value='${empty form.backlogs ? 0 : form.backlogs}'/>"
                                   aria-invalid="${not empty errors.backlogs ? 'true' : 'false'}">
                            <c:if test="${not empty errors.backlogs}"><div class="err"><c:out value="${errors.backlogs}"/></div></c:if>
                        </div>
                    </div>

                    <div class="field mb-0">
                        <label for="phone">Mobile number</label>
                        <input type="text" id="phone" name="phone" maxlength="10" inputmode="numeric" required
                               value="<c:out value='${form.phone}'/>"
                               aria-invalid="${not empty errors.phone ? 'true' : 'false'}">
                        <c:if test="${not empty errors.phone}"><div class="err"><c:out value="${errors.phone}"/></div></c:if>
                    </div>
                </fieldset>

                <div class="btn-row">
                    <button class="btn btn-primary" type="submit">Create account</button>
                    <a class="btn" href="${ctx}/login">I already have one</a>
                </div>
            </form>
        </div>
    </main>
</div>
</body>
</html>
