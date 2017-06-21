<%--
Knowage, Open Source Business Intelligence suite
Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

Knowage is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

Knowage is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>
<%@page import="it.eng.spagobi.commons.constants.SpagoBIConstants"%>

<%
	String spagoBiContext = request.getParameter(SpagoBIConstants.SBI_HOST)+request.getParameter(SpagoBIConstants.SBI_CONTEXT);
%>


<!-- angular -->
<!-- START-DEBUG -->
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular/angular.js"></script> 
<!-- END-DEBUG -->

<!-- angular material-->
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-animate/angular-animate.min.js"></script>
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-aria/angular-aria.min.js"></script>
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-sanitize/angular-sanitize.min.js"></script>
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-cookies/angular-cookies.min.js"></script>
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-messages/angular-messages.min.js"></script>
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/angular-material/angular-material.min.js"></script>
<link rel="stylesheet" href="<%=spagoBiContext%>/node_modules/angular-material/angular-material.min.css">

<!-- ngdraggable -->
<script type="text/javascript" src="<%=spagoBiContext%>/node_modules/ngdraggable/ngDraggable.js"></script>

<!-- angular list detail template -->
<script type="text/javascript" src="<%=spagoBiContext%>/js/src/angular_1.4/tools/commons/angular-list-detail/angularListDetail.js"></script>

<!-- main css -->
<link rel="stylesheet" type="text/css" href="<%=spagoBiContext%>/themes/commons/css/customStyle.css">

<!-- font-awesome -->
<link rel="stylesheet" href="<%=spagoBiContext%>/node_modules/font-awesome/css/font-awesome.min.css">		 

<%@include file="/WEB-INF/jsp/commons/angular/sbiModule.jspf"%>
<%@include file="/WEB-INF/jsp/commons/angular/sbiModuleAction.jspf"%>
