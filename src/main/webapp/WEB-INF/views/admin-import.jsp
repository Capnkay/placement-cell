<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Import" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>Bulk student import</h1>
        <p class="lede">Paste a batch of students as comma separated rows, one student per line.
            Every row is checked before anything is saved, so one bad line costs you that
            line, not the whole batch.</p>
    </div>
</div>

<div class="split">
    <div>
        <div class="card">
            <div class="card-head"><h2>Rows to import</h2></div>
            <div class="card-body">
                <div class="field">
                    <label for="csv">One student per line</label>
                    <textarea id="csv" rows="12" spellcheck="false" class="mono"
                              placeholder="Name,Email,Roll number,Branch,Graduating year,CGPA,Backlogs,Mobile">Priya Nair,priya.nair@campus.edu,CS22001,Computer Science,2027,8.4,0,9820100200
Karan Bhatt,karan.bhatt@campus.edu,IT22011,Information Technology,2027,7.1,1,9820100201
Not An Email,broken-address,IT22012,Information Technology,2027,7.9,0,9820100202</textarea>
                    <div class="help">
                        The third row above is deliberately broken so you can see a bad
                        address being refused rather than quietly saved.
                    </div>
                </div>

                <%-- The endpoint travels as a data attribute rather than being
                     written into a script block, so the page carries no inline
                     JavaScript and the content security policy can forbid it
                     outright. --%>
                <div class="btn-row">
                    <button class="btn btn-primary" id="run" type="button"
                            data-endpoint="${ctx}/admin/import?csrf=${csrf}"
                            data-register-url="${ctx}/admin/students">Import these rows</button>
                    <span class="small muted" id="state"></span>
                </div>
            </div>
        </div>

        <div class="card" id="resultCard" style="display:none">
            <div class="card-head"><h3>Result</h3></div>
            <div class="card-body" id="result"></div>
        </div>
    </div>

    <div>
        <div class="card">
            <div class="card-head"><h3>How rows are checked</h3></div>
            <div class="card-body">
                <ul class="reasons" style="padding-left:16px">
                    <li>Seven columns are required, the mobile number is the optional eighth.</li>
                    <li>The email must be a well formed address and must not already exist.</li>
                    <li>The branch must be one the college actually runs.</li>
                    <li>CGPA must fall between zero and ten, backlogs between zero and thirty.</li>
                    <li>A roll number already on the register is refused.</li>
                </ul>
                <p class="small muted mt-2 mb-0">
                    Every imported account gets the password
                    <span class="mono"><c:out value="${defaultPassword}"/></span>, which the
                    student should change from their profile at first sign in.
                </p>
            </div>
        </div>

        <div class="card">
            <div class="card-head"><h3>Branches accepted</h3></div>
            <div class="card-body">
                <c:forEach var="b" items="${branches}">
                    <span class="chip"><c:out value="${b}"/></span>
                </c:forEach>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
