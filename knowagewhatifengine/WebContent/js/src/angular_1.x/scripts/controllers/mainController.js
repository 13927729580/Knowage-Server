/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

var olapMod = angular.module('olap.controllers', [ 'olap.configuration',
		'olap.directives', 'olap.settings' ])
		
olapMod.config(['$mdThemingProvider', function($mdThemingProvider) {
    $mdThemingProvider.theme('knowage')
    $mdThemingProvider.setDefaultTheme('knowage');
 }]);

olapMod.controller("olapController", [ "$scope", "$timeout", "$window",
		"$mdDialog", "$http", '$sce', '$mdToast', '$mdSidenav',
		'sbiModule_messaging', 'sbiModule_restServices', 'sbiModule_translate','sbiModule_docInfo',
		'olapSharedSettings' ,olapFunction ]);

function olapFunction($scope, $timeout, $window, $mdDialog, $http, $sce,
		$mdToast, $mdSidenav, sbiModule_messaging, sbiModule_restServices,
		sbiModule_translate,sbiModule_docInfo, olapSharedSettings) {

	//VARIABLES
	var firstLoad = true;
	$scope.translate = sbiModule_translate;
	//selected members
	$scope.members = [];
	$scope.selectedMember = {};

	templateRoot = "/knowagewhatifengine/html/template";
	$scope.sendMdxDial = "/main/toolbar/sendMdx.html";
	$scope.saveSubObjectDial = "/main/savesubobject/saving_subobject_dialog.html";
	$scope.showMdxDial = "/main/toolbar/showMdx.html";
	$scope.sortSetDial = "/main/toolbar/sortingSettings.html";
	$scope.filterDial = "/main/filter/filterDialog.html"
	$scope.saveAsNew = "/main/toolbar/saveAsNew.html"

	$scope.filterDialogWidth = olapSharedSettings.getSettings().filterDialogWidth;
	$scope.filterDialogHeight = olapSharedSettings.getSettings().filterDialogHeight;
	$scope.allowEditingCC = olapSharedSettings.getSettings().disableManualEditingCC;
		
	$scope.minNumOfLetters = olapSharedSettings.getSettings().minSearchLength;
	$scope.searchText = "";
	$scope.searchSucessText="";
	$scope.showSearchInput = false;

	$scope.rows;
	$scope.maxRows = 3;
	$scope.topSliderNeeded;
	$scope.topStart = 0;
	$scope.tableSubsets ={};
	$scope.columns;
	$scope.maxCols = 5;
	$scope.leftSliderNeeded;
	$scope.leftStart = 0;

	$scope.olapToolbarButtons = [];
	$scope.whatifToolbarButtons = [];
	$scope.tableToolbarButtons = [];

	$scope.filterCardList = [];
	$scope.filterSelected = [];
	$scope.dtData = [];
	$scope.dtTree = [];
	$scope.dtMaxRows = 0;
	$scope.dtAssociatedLevels = [];
	$scope.formulasData = [];
	$scope.valuesArray = [];
	$scope.selectedMDXFunction = {};
	$scope.selectedMDXFunctionName = "";
	$scope.selectedTab = 0;
	$scope.olapDocName = sbiModule_docInfo.label;
	$scope.selectedCrossNavigationDocument = null;
	$scope.cookieArray = [];
	$scope.propertiesArray = [];

	$scope.finalFormula = null;
	$scope.isFilterSelected = false;
	$scope.filterAxisPosition;
	$scope.showMdxVar = "";

	$scope.draggedFrom = "";
	$scope.dragIndex;
	
	$scope.doneonce = false;
	$scope.level;
	$scope.data = [];
	$scope.loadedData = [];
	$scope.dataPointers = [];
	$scope.numVisibleFilters = 5;
	$scope.shiftNeeded;

	$scope.modelConfig;
	$scope.filterDialogToolbarName;

	$scope.showSiblings = true;
	$scope.sortingSetting;
	$scope.ready = true;
	$scope.sortingEnabled = false;
	$scope.crossNavigationEnabled = false;
	$scope.sortingModes = [ {
		'label' : 'basic',
		'value' : 'basic'
	}, {
		'label' : 'breaking',
		'value' : 'breaking'
	}, {
		'label' : 'count',
		'value' : 'count'
	} ];
	$scope.selectedSortingMode = 'basic';
	$scope.sortingCount = 10;
	$scope.saveSortingSettings = function() {
		if($scope.sortingCount<1||!$scope.sortingCount){
			sbiModule_messaging.showErrorMessage(sbiModule_translate.load('sbi.olap.sortingSetting.count.error'), 'Error');
			
		}else{
			$mdDialog.hide();
			$scope.sortDisable();
		}
		
	}
	$scope.loadingNodes = false;
	$scope.activeaxis;

	$scope.member;
	$scope.selecetedMultiHierUN;

	$scope.handleResponse = function(response) {
		$scope.tableSubsets=null;
		source = response.data;
		$scope.modelConfig = source.modelConfig;
		console.log($scope.modelConfig);
		$scope.table = $sce.trustAsHtml(source.table);
		$scope.tableSubsets=source.tables;
		
		$scope.columns = source.columns;
		$scope.rows = source.rows;
		$scope.columnsAxisOrdinal = source.columnsAxisOrdinal;
		$scope.filterCardList = source.filters;
		$scope.hasPendingTransformations = source.hasPendingTransformations;

		$scope.rowsAxisOrdinal = source.rowsAxisOrdinal;
		$scope.showMdxVar = source.mdxFormatted;
		$scope.formulasData = source.formulas;
		$scope.ready = true;

		$scope.wiGridNeeded = response.data.modelConfig.whatIfScenario; //arsenije
		if(firstLoad && $scope.modelConfig != undefined){
			firstLoad = false;
		}
		source = null;
	}

	$scope.sendModelConfig = function(modelConfig) {
		$scope.tableSubsets.length = 0;
		var sentStartRow = $scope.modelConfig.startRow;
		if ($scope.ready) {
			$scope.ready = false;
			sbiModule_restServices.promisePost(
					"1.0/modelconfig?SBI_EXECUTION_ID=" + JSsbiExecutionID, "",
					modelConfig).then(
					function(response) {
						
						$scope.table = $sce.trustAsHtml(response.data.table);
						
						
						
						$scope.modelConfig = response.data.modelConfig;
						$scope.tableSubsets=response.data.tables;
						
						$scope.ready = true;
						$scope.isScrolling = false;
						
								
					},
					function(response) {
						sbiModule_messaging.showErrorMessage(
								"An error occured while sending model config",
								'Error');
						$scope.ready = true;
					});

		}

	}

	$scope.startFrom = function(start) {
		if ($scope.ready) {
			$scope.ready = false;

			sbiModule_restServices.promiseGet(
					"1.0",
					'/member/start/1/' + start + '?SBI_EXECUTION_ID='
							+ JSsbiExecutionID).then(function(response) {
				$scope.table = $sce.trustAsHtml(response.data.table);
				$scope.ready = true;
				$scope.handleResponse(response);
			}, function(response) {
				sbiModule_messaging.showErrorMessage("error", 'Error');

			});
		}
	}

	/**
	 *Function for opening dialogs
	 **/
	$scope.showDialog = function(ev, path) {
		$mdDialog.show({
			scope : $scope,
			preserveScope : true,
			controllerAs : 'olapCtrl',
			templateUrl : templateRoot + path,
			targetEvent : ev,
			clickOutsideToClose : false
		});
	};

	$scope.closeDialog = function(e) {
		$mdDialog.hide();
	};

	$scope.getVersions = function() {
		sbiModule_restServices.promiseGet("1.0",
				'/version/?SBI_EXECUTION_ID=' + JSsbiExecutionID).then(
				function(response) {
					console.log(response);
					$scope.outputVersions = response.data;
				},
				function(response) {
					sbiModule_messaging.showErrorMessage("An error occured ",
							'Error');
				});
	};
	
}