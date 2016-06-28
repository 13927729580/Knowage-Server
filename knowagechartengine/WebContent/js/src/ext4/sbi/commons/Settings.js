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

Ext.ns("Sbi.settings");

Sbi.settings.cockpit = {
		layout : {
			useRelativeDimensions: false
		}

};

/**
 * Unicode for the flag sign for madnatory fields. This valu will be taken
 * for these two properties of this JS file:
 * 	- Sbi.settings.chart.configurationStep.unicodeValueForFlag
 * 	- Sbi.settings.chart.configurationStep.htmlForMandatoryFields
 * 
 * The first one is needed just for color GUI elements in order to handle
 * their labels (their behavior).
 * 
 * The second one is needed by all mandatory fields so they can take it for
 * their marking as mandatory (labels contain this unicode).
 * 
 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
 */
var unicodeForFlagValue = 9873;
var unicodeForFlagString = ' [&#' + unicodeForFlagValue + ']';

/**
 * Configurations for the Designer. 
 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
 */
Sbi.settings.chart = 
{
	/**
	 * Set the visibility state of the charts' border. This is used for 
	 * BAR, LINE, RADAR and SCATTER chart types.
	 */
	borderVisible: false,
		
	/**
	 * Customization for the left panel of the Designer (on the left side 
	 * of the separating line that splits the Designer into two parts) 
	 * that contains the GUI elements for the chart style, chart type and
	 * collections of measures and attributes.
	 */
	leftDesignerContainer:
	{
		/**
		 * The width percentage of all GUI items in the left Designer's 
		 * panel container. 
		 */
		widthPercentageOfItem: "100%"
	},
		
	configurationStep:
	{
		unicodeValueForFlag: unicodeForFlagValue, 
		unicodeForFlagString: unicodeForFlagString,
		
		/**
		 * Property for the sign that will represent mandatory fields on
		 * the Designer.
		 */
		htmlForMandatoryFields: "<span style='color: rgb(255, 0, 0);'> [&#" + unicodeForFlagValue + "]</span>",
		/**
		 * All GUI fields that appear inside the Designer (in its panels 
		 * and popups) should take this width.
		 */
		widthOfFields: 280,
		
		/**
		 * Padding for fields that are out of fieldsets and that are lying
		 * on three different positions: on the top, in the middle (inner)
		 * or at the end of the panel. 
		 */
		paddingOfTopFields: "0 0 5 0",			
		paddingOfInnerFields: "5 0 5 0",		
		paddingOfBottomFields: "5 0 0 0",
		
		/**
		 * Margin for fields that are inside of fieldsets and that are lying
		 * on three different positions.
		 */		
		marginOfTopFieldset: '5 0 2.5 0',
		marginOfTopFieldsetButtons: "5 0 5 10",
		
		marginOfInnerFieldset:  '2.5 0 2.5 0',		
		marginOfInnerFieldsetButtons: "0 0 5 10",
		
		marginOfBottomFieldset:  '2.5 0 5 0',
			
		/**
		 * Layout that all fields in the same panel should follow.
		 */
		layoutFieldsInMainPanel: 
    	{
        	type:'hbox',
        	align:"center"
		},
	
		/**
		 * Set the default dimension type for dimensions of the chart (height and 
		 * width) to be pixels. 
		 */
		defaultDimensionType: "pixels",
		
		/**
		 * Percentage/absolute value type for displaying tooltip and breadcrumb values for slices that are covered with mouse cursor.
		 * This is the default value that should be shown and taken when none is specified. So, by default show value of hovered slice
		 * in the SUNBURST chart in percents.
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		defaultPercAbsSliceValue: "percentage",
		
		/**
		 * Hide the unnecessary scale factor values, namely 'G', 'T', 'P', 'E', since we do not need them for the majority of our needs.
		 * If needed, the user should be able to change this property to "true" boolean value. This property can be found in the Series
		 * style configuration popup for a particular series item (icon with the scissors and triangle in the line of the series item),
		 * as the "Scale factor" combo box. [KNOWAGE-1108 JIRA ISSUE]
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		biggerScaleFactorsEnabled: false
	},
	
	/**
	 * Common configuration for the Axis style configuration and Serie style
	 * configuration popups.
	 */
	structureStep:
	{		
		axisAndSerieStyleConfigPopup:
		{
			width: 400,
			height: 500,
			overflowY: true,
			resizable: true
		},
		
		cockpitAxisAndSerieStyleConfigPopup:
		{
			height: 350
		},
		
		/**
		 * Height of the Y-axis panels and Preview panel on the Designer page.
		 * @author Danilo Ristovski (danristo, danilo.ristovski@mht.net)
		 */
		heightYAxisAndPreviewPanels: 330
	},
	
	parallel:
	{
		tooltip:
		{
			/**
			 * This parameter is used for the threshold that is used for
			 * determining if the text on the PARALLEL's tooltip is going
			 * to be black (when the tooltip's background is lighter color)
			 * or white (when the background is too dark).
			 */
			darknessThreshold: 0.7
		}
	}
};

Sbi.settings.mydata = {
	/**
	 * This options will set the default active
	 * filter used the first time the MyData page is opened
	 * Possibile values are:
	 * -'MyDataSet'
	 * -'EnterpriseDataSet'
	 * -'SharedDataSet'
	 * -'AllDataSet'
	 *
	 * Make attention that the default filter selected must be
	 * a visible filter, so for example if
	 * defaultFilter:'MyDataSet'
	 * showMyDataSetFilter must be true
	 */
	  defaultFilter: 'AllDataSet' //'UsedDataSet'

	, showUsedDataSetFilter: true
	, showMyDataSetFilter: false
	, showEnterpriseDataSetFilter: false
	, showSharedDataSetFilter: false
	, showAllDataSetFilter: true
	/**
	 * MY DATA :
	 * put false for previous behavior (all USER public ds + owned)
	 * put true for showing only owned datasets
	 */
	, showOnlyOwner: false
	/**
	 * Visibility of MyData tabs
	 */
	, showDataSetTab: true
	, showModelsTab: false
	/**
	 * Visibility of MyData TabToolbar (this hide the whole tab toolbar)
	 */
	, showTabToolbar: true
};

Sbi.settings.widgets = {
		//Details for specific file upload management (ex: img for document preview,...)
	   FileUploadPanel: {
			imgUpload: {
				maxSizeFile: 10485760
			  , directory: '/preview/images' //starting from /resources directory
			  , extFiles: ['BMP', 'IMG', 'JPG', 'PNG', 'GIF']
			}
		}
};