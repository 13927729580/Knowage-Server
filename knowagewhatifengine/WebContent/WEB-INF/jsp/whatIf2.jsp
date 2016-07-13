
<%-- ---------------------------------------------------------------------- --%>
<%-- JAVA IMPORTS               --%>
<%-- ---------------------------------------------------------------------- --%>
<%@include file="/WEB-INF/jsp/commons/angular/angularResource.jspf"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html ng-app="olapManager">
<head>
<%@include file="/WEB-INF/jsp/commons/angular/angularImport.jsp"%>
<%@include file="/WEB-INF/jsp/commons/olap/olapImport.jsp"%>
<link rel="stylesheet" type="text/css"	href="${pageContext.request.contextPath}/css/olap.css">
<link rel="stylesheet" type="text/css"	href="${pageContext.request.contextPath}/css/whatIf.css">
<link rel="stylesheet" type="text/css"	href="${pageContext.request.contextPath}/css/customStyle.css">
<title>OLAP</title>
<script>
	var JSsbiExecutionID = '<%= sbiExecutionID %>'
	var toolbarVisibleBtns = '<%= whatIfEngineInstance.getModelConfig().getToolbarVisibleButtons() %>'
	var toolbarClickedBtns = '<%= whatIfEngineInstance.getModelConfig().getToolbarClickedButtons() %>'
	var drillType = '<%= whatIfEngineInstance.getModelConfig().getDrillType() %>'
	var locker = '<%= whatIfEngineInstance.getModelConfig().getLocker() %>'
	var status = '<%= whatIfEngineInstance.getModelConfig().getStatus()%>'
</script>
</head>
<body ng-controller="olapController" >

<div loading ng-show="showEl" ng-class="showLoadingMask ? 'loadingMask' : 'loadingNoMask'">

	<md-progress-circular  style="top:50%;left:50%;z-index: 501;" md-mode="indeterminate" md-diameter="100%" ></md-progress-circular>
	
</div>

 
	<div layout="row">
		
		<div layout="column" flex=100>
		
			<main-toolbar ng-hide="true"></main-toolbar>
		
			<filter-panel></filter-panel>

			<olap-panel></olap-panel>
			
		</div>

		<div style="width:2px"></div>
		
		<sbi-side-nav></sbi-side-nav>
	</div>
</body>
</html>