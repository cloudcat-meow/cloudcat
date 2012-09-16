
<%@ page import="cloudstack.reporting.ReportRun" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'reportRun.label', default: 'ReportRun Details')}" />
		<title><g:message code="default.report.label" args="[entityName,latestReportRun.dateCreated]" /></title>
	</head>
	<body>
		<a href="#list-reportRunDetails" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
		</div>
		<div id="list-reportRunDetails" class="content scaffold-list" role="main">
			<h1><g:message code="default.report.label" args="[entityName,latestReportRun.dateCreated]" /></h1>
			<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
				<thead>
					<tr>
                                          <g:sortableColumn property="account" title="Account" />
                                          <g:each in="${instanceSizes}" status="i" var="instanceSize">
                                            <g:sortableColumn property="${instanceSize}" title="${instanceSize}" />
                                          </g:each>
                                          <g:sortableColumn property="total" title="Total Count" />
					</tr>
				</thead>
				<tbody>
				<g:each in="${accountInstances}" status="i" var="accountInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
                                          <td>${accountInstance["account"]}</td>
                                          <g:each in="${instanceSizes}" status="s" var="instanceSize">
                                            <td>${accountInstance[instanceSize]}</td>
                                          </g:each>
                                          <td>${accountInstance["total"]}</td>
					</tr>
				</g:each>
				</tbody>
			</table>
		</div>
	</body>
</html>
