<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="theme" value="${empty cookie.portal_theme ? 'light' : cookie.portal_theme.value}"/>
<!DOCTYPE html>
<html lang="en" data-theme="${theme}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in : Placement Cell</title>
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
            <h1>One board for every drive on campus.</h1>
            <p class="lede">Eligibility is worked out from your own record and applied
                the same way to everybody, so the board only shows you what you can
                actually apply for.</p>

            <ul class="auth-points">
                <li>
                    <svg class="i icon-chip"><use href="#i-briefcase"/></svg>
                    <span><strong>See where you stand</strong>
                        Drives you qualify for, and the exact reason when you do not.</span>
                </li>
                <li>
                    <svg class="i icon-chip"><use href="#i-arrow-right"/></svg>
                    <span><strong>Follow every application</strong>
                        From applied, through shortlist and interview, to the result.</span>
                </li>
                <li>
                    <svg class="i icon-chip"><use href="#i-file"/></svg>
                    <span><strong>Keep one record</strong>
                        CGPA, backlogs and your resume, in the place recruiters read.</span>
                </li>
            </ul>
        </div>
    </aside>

    <main class="auth-main">
        <div class="auth-box">
            <c:choose>
                <c:when test="${not empty welcomeEmail}">
                    <div class="welcome" role="status">
                        <span class="welcome-tick"><svg class="i"><use href="#i-check"/></svg></span>
                        <h2><c:choose>
                            <c:when test="${not empty welcomeName}">Welcome, <c:out value="${welcomeName}"/>. Your account is ready.</c:when>
                            <c:otherwise>Your account is ready.</c:otherwise>
                        </c:choose></h2>
                        <p class="welcome-who">
                            <span class="mono"><c:out value="${welcomeEmail}"/></span>
                            <span class="sep">&#183;</span> roll no. <c:out value="${welcomeRoll}"/>
                        </p>
                        <ol class="welcome-steps">
                            <li class="now"><span>1</span> Sign in below with the password you just chose</li>
                            <li><span>2</span> Add your resume on your profile</li>
                            <li><span>3</span> Apply to the drives you qualify for</li>
                        </ol>
                    </div>
                </c:when>
                <c:otherwise>
                    <h2>Sign in</h2>
                    <p class="lede">Use the email address the college has on record.</p>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty sessionScope.flashSuccess}">
                <div class="alert alert-ok">
                    <svg class="i"><use href="#i-check"/></svg>
                    <div><c:out value="${sessionScope.flashSuccess}"/></div>
                </div>
                <c:remove var="flashSuccess" scope="session"/>
            </c:if>

            <c:if test="${not empty reason}">
                <div class="alert alert-info">
                    <svg class="i"><use href="#i-info"/></svg>
                    <div><c:out value="${reason}"/></div>
                </div>
            </c:if>

            <c:if test="${not empty error}">
                <div class="alert alert-bad" role="alert">
                    <svg class="i"><use href="#i-alert"/></svg>
                    <div><c:out value="${error}"/></div>
                </div>
            </c:if>

            <form method="post" action="${ctx}/login" novalidate>
                <input type="hidden" name="csrf" value="${csrf}">
                <input type="hidden" name="next" value="<c:out value='${next}'/>">

                <div class="field">
                    <label for="email">Email address</label>
                    <input type="email" id="email" name="email" autocomplete="username"
                           inputmode="email" maxlength="190" required ${empty welcomeEmail ? 'autofocus' : ''}
                           placeholder="name@campus.edu"
                           value="<c:out value='${not empty email ? email : rememberedEmail}'/>"
                           aria-invalid="${not empty error ? 'true' : 'false'}">
                </div>

                <div class="field">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password"
                           autocomplete="current-password" maxlength="128" required
                           ${not empty welcomeEmail ? 'autofocus' : ''}
                           aria-invalid="${not empty error ? 'true' : 'false'}">
                </div>

                <div class="field">
                    <label class="check">
                        <input type="checkbox" name="remember" ${not empty rememberedEmail ? 'checked' : ''}>
                        <span>Remember my email on this device</span>
                    </label>
                </div>

                <button class="btn btn-primary btn-block" type="submit">${empty welcomeEmail ? 'Sign in' : 'Sign in to your new account'}</button>
            </form>

            <p class="small muted mt-2">
                New student? <a href="${ctx}/register">Create an account</a>
            </p>

            <c:if test="${not empty lastVisit}">
                <p class="small faint">Last signed in from this browser on
                    <c:out value="${lastVisit}"/>.</p>
            </c:if>

            <%--
                The seeded account list used to be printed here. It was removed:
                a sign in page that tells an anonymous visitor a working username
                and password is the same page with no sign in on it. The demo
                credentials live in README.md and in the run script output, which
                is where somebody setting the project up will already be looking.
            --%>

            <p class="small faint mt-2">
                Five wrong attempts lock an account for fifteen minutes.
            </p>

            <p class="small mt-2">
                <a href="${ctx}/theme?mode=${theme == 'dark' ? 'light' : 'dark'}&amp;back=/login">
                    Switch to the ${theme == 'dark' ? 'light' : 'dark'} palette</a>
            </p>
        </div>
    </main>
</div>
</body>
</html>
