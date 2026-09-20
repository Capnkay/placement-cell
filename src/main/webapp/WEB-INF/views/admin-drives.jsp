<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Drives" scope="request"/>
<%@ include file="layout-top.jspf" %>

<c:set var="edit" value="${editing}"/>
<c:set var="chosenBranches" value="${not empty form ? form.branches : edit.allowedBranches}"/>

<div class="page-head">
    <div>
        <h1>Placement drives</h1>
        <p class="lede">The eligibility rules entered here are the ones every student is measured
            against, so a change takes effect on the board straight away.</p>
    </div>
</div>

<c:if test="${empty companies}">
    <div class="alert alert-warn"><svg class="i"><use href="#i-alert"/></svg>
        <div>There are no companies on the register yet, and a drive must belong to one.
            <a href="${ctx}/admin/companies">Add a recruiter first</a>.</div>
    </div>
</c:if>

<div class="card">
    <div class="card-head">
        <h2>${empty edit ? 'Announce a drive' : 'Edit drive'}</h2>
        <c:if test="${not empty edit}">
            <div class="spacer"></div>
            <a class="small" href="${ctx}/admin/drives">Cancel and start a new one</a>
        </c:if>
    </div>
    <div class="card-body">
        <form method="post" action="${ctx}/admin/drives" novalidate>
            <input type="hidden" name="csrf" value="${csrf}">
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="id" value="${not empty form.id ? form.id : edit.id}">

            <div class="field-row">
                <div class="field">
                    <label for="companyId">Company</label>
                    <select id="companyId" name="companyId" required
                            aria-invalid="${not empty errors.companyId ? 'true' : 'false'}">
                        <option value="">Choose a recruiter</option>
                        <c:forEach var="co" items="${companies}">
                            <c:set var="sel" value="${not empty form ? form.companyId : edit.company.id}"/>
                            <option value="${co.id}" ${sel eq co.id ? 'selected' : ''}>
                                <c:out value="${co.name}"/></option>
                        </c:forEach>
                    </select>
                    <c:if test="${not empty errors.companyId}">
                        <div class="err"><c:out value="${errors.companyId}"/></div></c:if>
                </div>
                <div class="field">
                    <label for="jobRole">Role offered</label>
                    <input type="text" id="jobRole" name="jobRole" maxlength="120" required
                           placeholder="Software Engineer"
                           value="<c:out value='${not empty form ? form.jobRole : edit.jobRole}'/>"
                           aria-invalid="${not empty errors.jobRole ? 'true' : 'false'}">
                    <c:if test="${not empty errors.jobRole}">
                        <div class="err"><c:out value="${errors.jobRole}"/></div></c:if>
                </div>
                <div class="field">
                    <label for="location">Location</label>
                    <input type="text" id="location" name="location" maxlength="100" required
                           value="<c:out value='${not empty form ? form.location : edit.location}'/>"
                           aria-invalid="${not empty errors.location ? 'true' : 'false'}">
                    <c:if test="${not empty errors.location}">
                        <div class="err"><c:out value="${errors.location}"/></div></c:if>
                </div>
            </div>

            <fieldset>
                <legend>Eligibility</legend>
                <div class="field-row">
                    <div class="field">
                        <label for="packageLpa">Package in LPA</label>
                        <input type="number" id="packageLpa" name="packageLpa" step="0.1" min="0" max="200" required
                               value="<c:out value='${not empty form ? form.packageLpa : edit.packageLpa}'/>"
                               aria-invalid="${not empty errors.packageLpa ? 'true' : 'false'}">
                        <c:if test="${not empty errors.packageLpa}">
                            <div class="err"><c:out value="${errors.packageLpa}"/></div></c:if>
                    </div>
                    <div class="field">
                        <label for="minCgpa">Minimum CGPA</label>
                        <input type="number" id="minCgpa" name="minCgpa" step="0.01" min="0" max="10" required
                               value="<c:out value='${not empty form ? form.minCgpa : edit.minCgpa}'/>"
                               aria-invalid="${not empty errors.minCgpa ? 'true' : 'false'}">
                        <c:if test="${not empty errors.minCgpa}">
                            <div class="err"><c:out value="${errors.minCgpa}"/></div></c:if>
                    </div>
                    <div class="field">
                        <label for="maxBacklogs">Backlogs allowed</label>
                        <input type="number" id="maxBacklogs" name="maxBacklogs" min="0" max="30" required
                               value="<c:out value='${not empty form ? form.maxBacklogs : edit.maxBacklogs}'/>"
                               aria-invalid="${not empty errors.maxBacklogs ? 'true' : 'false'}">
                        <c:if test="${not empty errors.maxBacklogs}">
                            <div class="err"><c:out value="${errors.maxBacklogs}"/></div></c:if>
                    </div>
                </div>

                <div class="field mb-0">
                    <label>Eligible branches</label>
                    <div class="btn-row">
                        <c:forEach var="b" items="${branches}">
                            <label class="check">
                                <input type="checkbox" name="branches" value="<c:out value='${b}'/>"
                                       ${not empty chosenBranches and chosenBranches.contains(b) ? 'checked' : ''}>
                                <span><c:out value="${b}"/></span>
                            </label>
                        </c:forEach>
                    </div>
                    <c:if test="${not empty errors.branches}">
                        <div class="err"><c:out value="${errors.branches}"/></div></c:if>
                </div>
            </fieldset>

            <div class="field-row">
                <div class="field">
                    <label for="driveDate">Drive date</label>
                    <input type="date" id="driveDate" name="driveDate" required
                           value="<c:out value='${not empty form ? form.driveDate : edit.driveDate}'/>"
                           aria-invalid="${not empty errors.driveDate ? 'true' : 'false'}">
                    <c:if test="${not empty errors.driveDate}">
                        <div class="err"><c:out value="${errors.driveDate}"/></div></c:if>
                </div>
                <div class="field">
                    <label for="lastDate">Applications close</label>
                    <input type="date" id="lastDate" name="lastDate" required
                           value="<c:out value='${not empty form ? form.lastDate : edit.lastDate}'/>"
                           aria-invalid="${not empty errors.lastDate ? 'true' : 'false'}">
                    <c:choose>
                        <c:when test="${not empty errors.lastDate}">
                            <div class="err"><c:out value="${errors.lastDate}"/></div></c:when>
                        <c:otherwise><div class="help">Must be on or before the drive date.</div></c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div class="field">
                <label for="description">Process and description</label>
                <textarea id="description" name="description" maxlength="2000"
                          placeholder="Rounds, what candidates should prepare, anything the students should know."><c:out
                        value="${not empty form ? form.description : edit.description}"/></textarea>
                <c:if test="${not empty errors.description}">
                    <div class="err"><c:out value="${errors.description}"/></div></c:if>
            </div>

            <button class="btn btn-primary" type="submit" ${empty companies ? 'disabled' : ''}>
                ${empty edit ? 'Announce drive' : 'Save changes'}
            </button>
        </form>
    </div>
</div>

<div class="card">
    <div class="card-head">
        <h2>On the board</h2>
        <div class="spacer"></div>
        <span class="badge badge-neutral">${drives.size()} drive(s)</span>
    </div>
    <c:choose>
        <c:when test="${empty drives}">
            <div class="empty">
                <h3>Nothing announced yet</h3>
                <p class="mb-0">Use the form above to put the first drive on the board.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr><th>Company</th><th>Role</th><th>Rules</th><th>Dates</th>
                        <th>Applications</th><th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="d" items="${drives}">
                        <tr>
                            <td><strong><c:out value="${d.company.name}"/></strong>
                                <span class="sub"><c:out value="${d.location}"/></span></td>
                            <td><c:out value="${d.jobRole}"/>
                                <span class="sub num">${d.packageLpa} LPA</span></td>
                            <td class="small muted" style="min-width:200px">
                                CGPA <span class="num">${d.minCgpa}</span>
                                <span class="sep">&#183;</span>
                                <span class="num">${d.maxBacklogs}</span> backlog(s)
                                <div style="margin-top:3px">
                                    <c:forEach var="b" items="${d.branchList}">
                                        <span class="chip"><c:out value="${b}"/></span>
                                    </c:forEach>
                                </div>
                            </td>
                            <td class="small nowrap">${d.driveDate}
                                <span class="sub">closes ${d.lastDate}</span></td>
                            <td class="num">
                                <a href="${ctx}/admin/applications?drive=${d.id}">${applicationCounts[d.id]}</a>
                            </td>
                            <td><span class="badge ${d.status.badgeClass}">${d.status.label}</span></td>
                            <td class="right nowrap">
                                <a class="btn btn-sm" href="${ctx}/admin/drives?edit=${d.id}">Edit</a>
                                <form class="inline-form" method="post" action="${ctx}/admin/drives">
                                    <input type="hidden" name="csrf" value="${csrf}">
                                    <input type="hidden" name="id" value="${d.id}">
                                    <input type="hidden" name="action" value="${d.status == 'OPEN' ? 'close' : 'open'}">
                                    <button class="btn btn-sm" type="submit">
                                        ${d.status == 'OPEN' ? 'Close' : 'Reopen'}</button>
                                </form>
                                <form class="inline-form" method="post" action="${ctx}/admin/drives"
                                      data-confirm="Remove this drive and all ${applicationCounts[d.id]} application(s)?">
                                    <input type="hidden" name="csrf" value="${csrf}">
                                    <input type="hidden" name="id" value="${d.id}">
                                    <input type="hidden" name="action" value="delete">
                                    <button class="btn btn-sm btn-danger" type="submit">Remove</button>
                                </form>
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
