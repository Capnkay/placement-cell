<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Profile" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>My profile</h1>
        <p class="lede">Your academic record decides which drives you can apply to, so keep it current.
            Changing it recalculates every eligibility check immediately.</p>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head"><h2>Academic details</h2></div>
            <div class="card-body">
                <form method="post" action="${ctx}/app/profile" novalidate>
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="action" value="details">

                    <div class="field-row">
                        <div class="field">
                            <label>Roll number</label>
                            <input type="text" value="<c:out value='${profile.rollNo}'/>" disabled>
                            <div class="help">Only the placement cell can change this.</div>
                        </div>
                        <div class="field">
                            <label for="branch">Branch</label>
                            <select id="branch" name="branch" required
                                    aria-invalid="${not empty detailErrors.branch ? 'true' : 'false'}">
                                <c:forEach var="b" items="${branches}">
                                    <option value="<c:out value='${b}'/>" ${profile.branch == b ? 'selected' : ''}>
                                        <c:out value="${b}"/></option>
                                </c:forEach>
                            </select>
                            <c:if test="${not empty detailErrors.branch}">
                                <div class="err"><c:out value="${detailErrors.branch}"/></div></c:if>
                        </div>
                    </div>

                    <div class="field-row">
                        <div class="field">
                            <label for="batchYear">Graduating year</label>
                            <input type="number" id="batchYear" name="batchYear" required
                                   value="${profile.batchYear}"
                                   aria-invalid="${not empty detailErrors.batchYear ? 'true' : 'false'}">
                            <c:if test="${not empty detailErrors.batchYear}">
                                <div class="err"><c:out value="${detailErrors.batchYear}"/></div></c:if>
                        </div>
                        <div class="field">
                            <label for="cgpa">CGPA</label>
                            <input type="number" id="cgpa" name="cgpa" step="0.01" min="0" max="10" required
                                   value="${profile.cgpa}"
                                   aria-invalid="${not empty detailErrors.cgpa ? 'true' : 'false'}">
                            <c:if test="${not empty detailErrors.cgpa}">
                                <div class="err"><c:out value="${detailErrors.cgpa}"/></div></c:if>
                        </div>
                        <div class="field">
                            <label for="backlogs">Live backlogs</label>
                            <input type="number" id="backlogs" name="backlogs" min="0" max="30" required
                                   value="${profile.backlogs}"
                                   aria-invalid="${not empty detailErrors.backlogs ? 'true' : 'false'}">
                            <c:if test="${not empty detailErrors.backlogs}">
                                <div class="err"><c:out value="${detailErrors.backlogs}"/></div></c:if>
                        </div>
                    </div>

                    <div class="field">
                        <label for="phone">Mobile number</label>
                        <input type="text" id="phone" name="phone" maxlength="10" inputmode="numeric" required
                               value="<c:out value='${profile.phone}'/>"
                               aria-invalid="${not empty detailErrors.phone ? 'true' : 'false'}">
                        <c:if test="${not empty detailErrors.phone}">
                            <div class="err"><c:out value="${detailErrors.phone}"/></div></c:if>
                    </div>

                    <button class="btn btn-primary" type="submit">Save details</button>
                </form>
            </div>
        </div>

        <div class="card">
            <div class="card-head"><h2>Change password</h2></div>
            <div class="card-body">
                <form method="post" action="${ctx}/app/profile" novalidate>
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="action" value="password">

                    <div class="field">
                        <label for="currentPassword">Current password</label>
                        <input type="password" id="currentPassword" name="currentPassword"
                               autocomplete="current-password" required
                               aria-invalid="${not empty passwordErrors.currentPassword ? 'true' : 'false'}">
                        <c:if test="${not empty passwordErrors.currentPassword}">
                            <div class="err"><c:out value="${passwordErrors.currentPassword}"/></div></c:if>
                    </div>

                    <div class="field-row">
                        <div class="field">
                            <label for="newPassword">New password</label>
                            <input type="password" id="newPassword" name="newPassword"
                                   autocomplete="new-password" minlength="8" required
                                   aria-invalid="${not empty passwordErrors.newPassword ? 'true' : 'false'}">
                            <c:choose>
                                <c:when test="${not empty passwordErrors.newPassword}">
                                    <div class="err"><c:out value="${passwordErrors.newPassword}"/></div></c:when>
                                <c:otherwise><div class="help">Eight characters or more, letters and digits.</div></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="field">
                            <label for="confirmPassword">Repeat new password</label>
                            <input type="password" id="confirmPassword" name="confirmPassword"
                                   autocomplete="new-password" minlength="8" required
                                   aria-invalid="${not empty passwordErrors.confirmPassword ? 'true' : 'false'}">
                            <c:if test="${not empty passwordErrors.confirmPassword}">
                                <div class="err"><c:out value="${passwordErrors.confirmPassword}"/></div></c:if>
                        </div>
                    </div>

                    <button class="btn" type="submit">Change password</button>
                </form>
            </div>
        </div>
    </div>

    <div>
        <div class="card">
            <div class="card-head"><h3>Resume</h3></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty profile.resumeFile}">
                        <div class="alert alert-warn mb-0"><svg class="i"><use href="#i-alert"/></svg>
                            <div>No resume on file. Recruiters ask for one before shortlisting.</div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-ok"><svg class="i"><use href="#i-check"/></svg>
                            <div>A resume is on file and the placement cell can download it.</div>
                        </div>
                        <a class="btn btn-sm" href="${ctx}/app/resume">Download the copy on file</a>
                    </c:otherwise>
                </c:choose>

                <%--
                    The token travels in the query string on this form, not only in a
                    hidden field. A multipart body is parsed by the servlet rather than
                    the filter, so a value on the URL is the one the guard can always read.
                --%>
                <form class="mt-2" method="post" enctype="multipart/form-data"
                      action="${ctx}/app/profile?csrf=${csrf}">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="action" value="resume">
                    <div class="field">
                        <label for="resume">Upload a new resume</label>
                        <input type="file" id="resume" name="resume" accept=".pdf,.doc,.docx" required>
                        <div class="help">PDF, DOC or DOCX, up to two megabytes.
                            A new upload replaces the old one.</div>
                    </div>
                    <button class="btn btn-primary btn-sm" type="submit">Upload</button>
                </form>
            </div>
        </div>

        <div class="card">
            <div class="card-head"><h3>Account</h3></div>
            <div class="card-body">
                <dl class="kv">
                    <dt>Name</dt><dd><c:out value="${me.fullName}"/></dd>
                    <dt>Email</dt><dd class="mono"><c:out value="${me.email}"/></dd>
                    <dt>Role</dt><dd><c:out value="${me.role.label}"/></dd>
                </dl>
                <p class="small muted mt-1 mb-0">
                    Your password is stored as a salted PBKDF2 digest. Nobody, including the
                    placement cell, can read it back.
                </p>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
