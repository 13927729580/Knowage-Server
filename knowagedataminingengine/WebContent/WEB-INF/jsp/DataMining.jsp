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

<%-- 
author:...
--%>

<%@ page language="java" 
	     contentType="text/html; charset=UTF-8" 
	     pageEncoding="UTF-8"%>	


<%-- ---------------------------------------------------------------------- --%>
<%-- JAVA IMPORTS															--%>
<%-- ---------------------------------------------------------------------- --%>
<%@page import="it.eng.spago.configuration.*"%>
<%@page import="it.eng.spago.base.*"%>
<%@page import="it.eng.spagobi.engines.datamining.DataMiningEngineConfig"%>
<%@page import="it.eng.spagobi.engines.datamining.DataMiningEngineInstance"%>
<%@page import="it.eng.spagobi.utilities.engines.EngineConstants"%>
<%@page import="it.eng.spagobi.commons.bo.UserProfile"%>
<%@page import="it.eng.spago.security.IEngUserProfile"%>
<%@page import="it.eng.spagobi.commons.constants.SpagoBIConstants"%>
<%@page import="java.util.Locale"%>
<%@page import="it.eng.spagobi.services.common.EnginConf"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<%@page import="it.eng.spagobi.utilities.engines.rest.ExecutionSession"%>
<%@page import="it.eng.spagobi.engines.datamining.model.DataMiningDataset"%>

<%-- ---------------------------------------------------------------------- --%>
<%-- JAVA CODE 																--%>
<%-- ---------------------------------------------------------------------- --%>
<%
	DataMiningEngineInstance dataMiningEngineInstance;
	String documentId;
	String documentLabel;
	UserProfile profile;
	Locale locale;
	String isFromCross;
	String spagobiServerHost;
	String spagobiContext;
	String spagobiSpagoController;
	String executionOutput;
	Boolean doUploadDatasets= false;
	
	ExecutionSession es = new ExecutionSession(request, request.getSession());
	
	dataMiningEngineInstance = (DataMiningEngineInstance)es.getAttributeFromSession(EngineConstants.ENGINE_INSTANCE );
	//documentId = (String)es.getAttributeFromSession(EngineConstants.DOCUMENT_ID );
	documentLabel= (String)es.getAttributeFromSession(EngineConstants.ENV_DOCUMENT_LABEL);	
	
	profile = (UserProfile)dataMiningEngineInstance.getEnv().get(EngineConstants.ENV_USER_PROFILE);
	locale = (Locale)dataMiningEngineInstance.getEnv().get(EngineConstants.ENV_LOCALE);
	
	isFromCross = (String)dataMiningEngineInstance.getEnv().get("isFromCross");
	if (isFromCross == null) {
		isFromCross = "false";
	}
    
    spagobiServerHost = request.getParameter(SpagoBIConstants.SBI_HOST);
    spagobiContext = request.getParameter(SpagoBIConstants.SBI_CONTEXT);
    spagobiSpagoController = request.getParameter(SpagoBIConstants.SBI_SPAGO_CONTROLLER);
    
    
%>


<%-- ---------------------------------------------------------------------- --%>
<%-- HTML	 																--%>
<%-- ---------------------------------------------------------------------- --%>
<!doctype html>
<html ng-app="dataMiningApp">
	
	<head>
		<%@include file="commons/angular/angularImport.jsp"%>
		<%@include file="commons/angular/dataminingModule.jsp"%>
		<%@include file="commons/includeMessageResource.jspf" %>
		
<%-- 
		<script type="text/javascript" src="/knowagedataminingengine/js/src/angular_1.4/datamining/dataminingController.js"></script>
		<link rel="stylesheet" type="text/css" href="/knowagedataminingengine/css/generalStyle.css">
		<link rel="stylesheet" type="text/css" href="/knowagedataminingengine/css/datamining.css">	
 --%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/src/angular_1.4/datamining/dataminingController.js"></script>
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/generalStyle.css">
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/datamining.css">	
	</head>
<body class="bodyStile" ng-controller="Controller" ng-class="{'loading-body' : pendingRequest > 0}" ng-cloak>
	<div id="popupContainer">
		<md-content class="no-margin-bottom" ng-hide = "pendingRequest > 0" layout-margin layout="row" layout-align = "start stretch ">
			<md-input-container flex="50">
				<md-select placeholder="Select Command" ng-model="cmd" ng-change="calculateResult(cmd)">
		          <md-option ng-repeat="cmd in commands" ng-value="cmd">
		            {{cmd.label}}
		          </md-option>
		        </md-select>
			</md-input-container>
			<md-button layout="column" class="md-fab md-raised" ng-click="toogleVariableForm(cmd)" ng-if = "cmd.variables !== null && cmd.variables !== 'null' && cmd.variables !== undefined && cmd.variables.length > 0">
				<md-tooltip md-direction="bottom" ng-if="!variableForm">
				    Set Command Variables
				</md-tooltip>
				<md-icon ng-show="!variableForm" class="fa fa-cogs center-ico"></md-icon>
					<md-tooltip md-direction="bottom" ng-if="variableForm">
					    Return Output Page
					</md-tooltip>
				<md-icon ng-show="variableForm" class="fa fa-undo center-ico"></md-icon>	
			</md-button>
		</md-content>
		<div class="loading-message" ng-if="pendingRequest > 0" layout="row" layout-align="center center">
				<i class="fa fa-spinner fa-spin fa-5x"></i>
				<span class="loading-padding">
				{{translate.load("sbi.dm.execution.loading");}} 
				</span>
		</div>
		<md-content class="no-margin-top" ng-hide = "pendingRequest > 0" layout="column" layout-margin>
			<md-content class="no-margin-top" ng-if="variableForm" layout-wrap>
				<div class = "border-container" >
					<md-toolbar class="md-blue minihead element-border">
					    <div class="md-toolbar-tools">
					 		Update variables of command '<b>{{cmd.label}}</b>' 
					 	</div>
					 </md-toolbar>
				 	<div ng-repeat = "variable in cmd.variables">
						 <md-content layout='row' layout-wrap layout-align="center center">
							 <md-input-container flex='70'>
							 		<label>
							 		 <b>{{ variable.name }}</b> 
							 		</label>
								 	<input type='text' ng-model="variable.currentVal" aria-label = "variable value"/>
							 </md-input-container>
						 	<md-button class="md-button md-raised md-ExtraMini" arial-label="Update" ng-click="setVariable(cmd, undefined, variable, 'set', 'command')" ng-disabled="variable.currentVal.length <= 0">
			 				 	Set
							</md-button>
							<md-button class="md-button md-raised md-ExtraMini" arial-label="Reset" ng-click="setVariable(cmd, undefined, variable, 'reset','command')" ng-disabled="variable.currentVal == variable.defaultVal">
			 				 	Reset
							</md-button>
						 </md-content>
					</div>
				</div>
			</md-content>
			<md-content class="no-margin-top no-margin-bottom" ng-if = "!variableForm && visibleUploadButton && cmd !== undefined" layout='row' layout-wrap>
				<div flex="30">
					<md-select placeholder="Select Dataset to Upload" ng-model="dataset">
						<md-option ng-value="ds" ng-repeat="ds in datasets[cmd.name]" >
							{{ds.label}}
						</md-option>
					</md-select>
				</div>
				<div ng-if="dataset" layout="row" layout-align="start center">
					<div ng-if="visibilityRerunButton">
						<md-button class="md-button md-raised md-ExtraMini md-larger" aria-label="Rerun Script" ng-click="rerunScript(cmd)">
							 Run Script
						</md-button>
					</div>
					<!-- Simulate button and link the click event with the input type='file' -->
					<label class="md-button md-raised md-ExtraMini" md-ink-ripple for="fileInput">
						<span >File</span>
					</label>
					<input id="fileInput" type="file" class="ng-hide" aria-label="fileName" onchange='angular.element(this).scope().setFileName(this)'>
					<md-input-container >
						<input type='text' ng-disabled="true" ng-model="fileName" aria-label="fileName"/>
					</md-input-container>
					<md-button class="md-fab md-raised" ng-disabled="!fileName || fileName.length == 0" arial-label="Upload File" ng-click="uploadFile(cmd,dataset)">
						<md-icon class="fa fa-upload center-ico"></md-icon>
						<md-tooltip md-direction="bottom">
	         					Upload File
	       				</md-tooltip>
					</md-button>
				</div>
			</md-content>
		
			<md-content ng-if="!variableForm" layout-wrap>
				<md-tabs class="mini-tabs" md-selected="idx_output" layout="column" md-dynamic-height> 
					<md-tab class="mini-tabs" ng-repeat="out in cmd.outputs" label="{{out.ouputLabel}}" md-on-select="getOutputResultFromTabClick(cmd,out)">
						<md-content layout="column" layout-padding>
							<md-content class="no-padding-top">
								<md-content class="no-margin-top" layout = 'row' layout-wrap>
									<div ng-if = "out.variables !== 'null' && out.variables !== undefined && out.variables.length > 0">
										<md-button class="md-button md-raised md-ExtraMini md-larger" ng-click = "toogleOuputVariables()" arial-label = "button output variable" >
											<span style="font-size: 65%;">Outputs Variables</span>
										</md-button>
									</div>
								</md-content>
								
								<div class = "border-container little-margin-bottom" ng-if = "visibleOuputVariables && out.variables !== undefined && out.variables.length > 0" >
									<md-toolbar class="md-blue minihead element-border">
									    <div class="md-toolbar-tools">
									 		Update variables of output '<b>{{out.ouputLabel}}</b>'
									 	</div>
									 </md-toolbar>
									 <div ng-repeat = "variable in out.variables">
										 <md-content layout='row' layout-wrap layout-align="center center">
											 <md-input-container flex='70'>
											    <label><b>{{ variable.name }}</b></label>
											 	<input type='text' ng-model="variable.currentVal" aria-label = "variable value"/>
											 </md-input-container>
										 	<md-button class="md-button md-raised md-ExtraMini" arial-label="Update" ng-click="setVariable(cmd, out, variable, 'set','output')" ng-disabled="variable.currentVal.length <= 0">
							 				 Set
											</md-button>
											<md-button class="md-button md-raised md-ExtraMini" arial-label="Reset" ng-click="setVariable(cmd, out, variable, 'reset','output')" ng-disabled="variable.currentVal == variable.defaultVal">
							 				 Reset
											</md-button>
										 </md-content>
									 </div>
								</div>
								<div class = "border-container">
									<md-toolbar class="minihead element-border" ng-class="{'error-toolbar' : results[cmd.name][out.outputName].error}">
										    <div class="md-toolbar-tools">
										 		<span ng-if="!results[cmd.name][out.outputName].error">Results</span>
										 		<span ng-if="results[cmd.name][out.outputName].error">Error</span> 
										 	</div>
								 	</md-toolbar>
								 	<md-content layout-margin layout-align="stretch center">
								 		<div ng-if="results[cmd.name][out.outputName].error">
								 			{{results[cmd.name][out.outputName].error}}
								 		</div>
										<div class="div-image" ng-if = "results[cmd.name][out.outputName].outputType == 'image' ">
											<div layout="row" layout-align="center center">
												<md-input-container>
													 <label>{{translate.load("sbi.datamining.img.width");}} (px)</label>
													<input type="number" min="0" ng-model="imgWidth">
												</md-input-container>
												<div flex="5"></div>
												<md-input-container>
													<label>{{translate.load("sbi.datamining.img.height");}} (px)</label>
													<input type="number" min="0" ng-model="imgHeight">
												</md-input-container>
											</div>
											<div layout="row" layout-align="center center" ng-if="results[cmd.name][out.outputName].result != null && results[cmd.name][out.outputName].result.length > 0">
												<img ng-style="{'width':imgWidth+'px','height':imgHeight+'px'}"  alt="Result for '{{results[cmd.name][out.outputName].plotName}}'" src="{{results[cmd.name][out.outputName].result}}" />
												<br>
											</div>
											<!-- 
											<div ng-bind-html="results[cmd.name][out.outputName].html">
											</div>
											-->
										</div>
										<div layout-padding class="div-text" ng-if = "results[cmd.name][out.outputName].outputType == 'text' ">
											<h3 class="md-subhead">	
												{{out.outputName}} = {{results[cmd.name][out.outputName].result}}
											</h3>
										</div>
										
										<div layout-padding class="div-text" ng-if = "results[cmd.name][out.outputName].outputType == 'Dataset'|| results[cmd.name][out.outputName].outputType == 'dataset' || results[cmd.name][out.outputName].outputType == 'spagobi_ds' ">
											<h3 class="md-subhead">	
												{{out.outputName}} = SpagoBi dataset saved, visible from Data Set section in Document Browser, with label: &nbsp; {{results[cmd.name][out.outputName].result}}
											</h3>
										</div>
										
										
									</md-content>
								</div>
								
							</md-content>
						</md-content>
						<md-content style="height : 20px;">
						<!-- This content avoids to cut the previous contents, so the results are well formatted -->
						</md-content>
					</md-tab>
				</md-tabs>
			</md-content>
		</md-content>
	</div>
</body>	
	
</html>
