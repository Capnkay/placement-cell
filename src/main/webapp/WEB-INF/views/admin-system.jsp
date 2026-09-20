<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="System" scope="request"/>
<%@ include file="layout-top.jspf" %>

<div class="page-head">
    <div>
        <h1>System</h1>
        <p class="lede">What the container is running, what the naming service has bound, and the
            counters held by the singleton session bean since this deployment started.</p>
    </div>
</div>

<div class="grid cols-2">
    <div class="card">
        <div class="card-head"><h3>Container</h3></div>
        <div class="card-body">
            <dl class="kv">
                <c:forEach var="e" items="${container}">
                    <dt><c:out value="${e.key}"/></dt>
                    <dd class="small"><c:out value="${e.value}"/></dd>
                </c:forEach>
                <dt>Singleton started</dt>
                <dd class="small"><c:out value="${startedAt}"/></dd>
            </dl>
        </div>
    </div>

    <div class="card">
        <div class="card-head"><h3>Counters</h3></div>
        <div class="card-body">
            <dl class="kv">
                <c:forEach var="e" items="${counters}">
                    <dt><c:out value="${e.key}"/></dt>
                    <dd class="num"><c:out value="${e.value}"/></dd>
                </c:forEach>
            </dl>
            <p class="small muted mt-2 mb-0">
                Session counts come from an HttpSessionListener, which the container calls
                on creation and on invalidation or timeout. The rest are held by the
                singleton bean behind container managed read and write locks.
            </p>
        </div>
    </div>
</div>

<div class="card">
    <div class="card-head">
        <h3>JNDI namespace</h3>
        <div class="spacer"></div>
        <span class="badge badge-neutral">${jndi.size()} names looked up</span>
    </div>
    <div class="table-wrap">
        <table>
            <thead><tr><th>Name</th><th>What the naming service returned</th></tr></thead>
            <tbody>
            <c:forEach var="e" items="${jndi}">
                <tr>
                    <td class="mono small"><c:out value="${e.key}"/></td>
                    <td class="mono small muted"><c:out value="${e.value}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
    <div class="card-body">
        <p class="small muted mb-0">
            These were resolved at startup with an <span class="mono">InitialContext</span>
            lookup inside the singleton bean. The datasource name is the same one
            <span class="mono">persistence.xml</span> hands to Hibernate, which is how the
            application asks for a connection pool by name instead of carrying a JDBC URL.
        </p>
    </div>
</div>

<%@ include file="layout-bottom.jspf" %>
