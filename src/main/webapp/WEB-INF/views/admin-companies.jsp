<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Companies" scope="request"/>
<%@ include file="layout-top.jspf" %>

<c:set var="edit" value="${editing}"/>

<div class="page-head">
    <div>
        <h1>Recruiter register</h1>
        <p class="lede">Every drive belongs to a company on this list. A company cannot be removed
            while it still has drives on the board.</p>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head">
                <h2>Companies</h2>
                <div class="spacer"></div>
                <span class="badge badge-neutral">${companies.size()} on record</span>
            </div>
            <c:choose>
                <c:when test="${empty companies}">
                    <div class="empty">
                        <h3>No companies yet</h3>
                        <p class="mb-0">Add the first recruiter with the form beside this list.</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Company</th><th>Sector</th><th>Contact</th><th>Drives</th><th></th></tr></thead>
                            <tbody>
                            <c:forEach var="co" items="${companies}">
                                <tr>
                                    <td>
                                        <strong><c:out value="${co.name}"/></strong>
                                        <c:if test="${not empty co.website}">
                                            <span class="sub"><a href="<c:out value='${co.website}'/>"
                                                target="_blank" rel="noreferrer noopener">
                                                <c:out value="${co.website}"/></a></span>
                                        </c:if>
                                    </td>
                                    <td><c:out value="${co.sector}"/></td>
                                    <td class="mono small"><c:out value="${co.hrEmail}"/></td>
                                    <td class="num">${driveCounts[co.id]}</td>
                                    <td class="right nowrap">
                                        <a class="btn btn-sm" href="${ctx}/admin/companies?edit=${co.id}">Edit</a>
                                        <form class="inline-form" method="post" action="${ctx}/admin/companies"
                                              data-confirm="Remove ${co.name} from the register?">
                                            <input type="hidden" name="csrf" value="${csrf}">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${co.id}">
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
    </div>

    <div>
        <div class="card">
            <div class="card-head">
                <h3>${empty edit ? 'Add a company' : 'Edit company'}</h3>
                <c:if test="${not empty edit}">
                    <div class="spacer"></div>
                    <a class="small" href="${ctx}/admin/companies">Cancel</a>
                </c:if>
            </div>
            <div class="card-body">
                <form method="post" action="${ctx}/admin/companies" novalidate>
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="id" value="${not empty form.id ? form.id : edit.id}">

                    <div class="field">
                        <label for="name">Company name</label>
                        <input type="text" id="name" name="name" maxlength="120" required
                               value="<c:out value='${not empty form ? form.name : edit.name}'/>"
                               aria-invalid="${not empty errors.name ? 'true' : 'false'}">
                        <c:if test="${not empty errors.name}"><div class="err"><c:out value="${errors.name}"/></div></c:if>
                    </div>

                    <div class="field">
                        <label for="sector">Sector</label>
                        <input type="text" id="sector" name="sector" maxlength="80" required
                               placeholder="Product Engineering"
                               value="<c:out value='${not empty form ? form.sector : edit.sector}'/>"
                               aria-invalid="${not empty errors.sector ? 'true' : 'false'}">
                        <c:if test="${not empty errors.sector}"><div class="err"><c:out value="${errors.sector}"/></div></c:if>
                    </div>

                    <div class="field">
                        <label for="hrEmail">Recruiter contact email</label>
                        <input type="email" id="hrEmail" name="hrEmail" maxlength="190" required
                               value="<c:out value='${not empty form ? form.hrEmail : edit.hrEmail}'/>"
                               aria-invalid="${not empty errors.hrEmail ? 'true' : 'false'}">
                        <c:if test="${not empty errors.hrEmail}"><div class="err"><c:out value="${errors.hrEmail}"/></div></c:if>
                    </div>

                    <div class="field">
                        <label for="website">Website</label>
                        <input type="text" id="website" name="website" maxlength="200"
                               placeholder="https://example.com"
                               value="<c:out value='${not empty form ? form.website : edit.website}'/>"
                               aria-invalid="${not empty errors.website ? 'true' : 'false'}">
                        <c:choose>
                            <c:when test="${not empty errors.website}">
                                <div class="err"><c:out value="${errors.website}"/></div></c:when>
                            <c:otherwise><div class="help">Optional.</div></c:otherwise>
                        </c:choose>
                    </div>

                    <button class="btn btn-primary" type="submit">
                        ${empty edit ? 'Add company' : 'Save changes'}
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
