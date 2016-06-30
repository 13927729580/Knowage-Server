var app = angular.module('calcManager', ['ngMaterial','ngMessages'/*,'angular_table'*/]);


//IMPORTANTE inserisci questa configurazione, in modo da abilitare il mio tema
app.config(function($mdThemingProvider) {
  $mdThemingProvider.theme('kn')
  .primaryPalette('indigo');
  $mdThemingProvider.setDefaultTheme('kn');
});



app.controller('calculatorRuntimeCtrl', ["$scope","$log","$mdDialog","$http","$location",calcRuntimeManagerFunction]);

function calcRuntimeManagerFunction($scope,$log,$mdDialog,$http,$location)
{
	var self=this;

//-------------------------Utility functions definition--------------------------

	Number.prototype.formatMoney = function(places, thousand, decimal, roundToHundreds) {
		places = !isNaN(places = Math.abs(places)) ? places : 2;
		thousand = thousand || ",";
		decimal = decimal || ".";

		var defaultRoundToHundreds=true;
		roundToHundreds = roundToHundreds || defaultRoundToHundreds; // if roundToHundreds isn't defined, sets true

		var number = this;
		if(roundToHundreds)
		{
			number=Math.ceil(number/100)*100;
		}
	    var	negative = number < 0 ? "-" : "";
	    var	i = parseInt(number = Math.abs(+number || 0).toFixed(places), 10) + "";
	    var	j = (j = i.length) > 3 ? j % 3 : 0;
	    return negative + (j ? i.substr(0, j) + thousand : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + thousand) + (places ? decimal + Math.abs(number - i).toFixed(places).slice(2) : "");

	};


	self.isUndefined = function(thing)
	{
	    return (typeof thing === "undefined" || thing.length===0);
	}

    self.toggle = function (item, list) {
        var idx = list.indexOf(item);
        if (idx > -1) list.splice(idx, 1);
        else list.push(item);
    };

    self.exists = function (item, list) {
        return list.indexOf(item) > -1;
    };



    self.arrayContains = function (a, obj) {
        var i = a.length;
        while (i--) {
           if (a[i] === obj) {
               return true;
           }
        }
        return false;
    };



   /* var urlParam = function(name, w){
        w = w || window;
        var rx = new RegExp('[\&|\?]'+name+'=([^\&\#]+)'),
            val = w.location.search.match(rx);
        return !val ? '':val[1];
    }*/




  //AGGIUNTE DI DAVIDE
	self.selected = [];
	self.addProduct = function(product){
		var i = self.selected.indexOf(product);
		if( i == -1){
			self.selected.push(product);
			var itemDet=self.itemDetail[product];
			itemDet["selected"]="selected";
		}else{
			self.selected.splice(i,1);
			var itemDet=self.itemDetail[product];
			itemDet["selected"]="";
		}
		$log.info("Selected products: ",self.selected);

	}




//-------------------------Utility variables definition--------------------------

	self.items=["BD","SI","ER","LI","PM","PA","OD","EI"];
	self.itemTooltip={"BD":"Big Data", "SI":"Smart Intelligence", "ER":"Enterprise Reporting", "LI":"Location Intelligence", "PM":"Performance Management", "PA":"Predictive Analisys","OD":"Open Data","EI":"Embedded Intelligence"}
	self.itemDetail={
		"BD":	{ "name":"Big Data","selected" : "" },
		"SI":	{ "name":"Smart Intelligence","selected" : "" },
		"ER":	{ "name":"Enterprise Reporting","selected" : "" },
		"LI":	{ "name":"Location Intelligence","selected" : "" },
		"PM":	{ "name":"Performance Management","selected" : "" },
		"PA":	{ "name":"Predictive Analisys","selected" : "" },
		"OD":	{ "name":"Open Data","selected" : "" },
		"EI":	{ "name":"Embedded Intelligence","selected" : "" }
	}


    self.cores=['4','8','12','16','20','24'];
    self.selectedNumCores="8";
    self.showSimpleResults=false;
    self.showTableResults=false;
	self.showOEMintResults=false;
	self.showCalculate=true;
	self.productsOEMintDataResults=false;
	self.productsOEMintDataSilver=[];
	self.productsOEMintDataGold=[];


	self.body={};
	//self.body.modality=urlParam('MODALITY');
	self.body.modality=window.location.search.substring(10);
	$log.info("modalty: ",window.location.search.substring(10));


//---------------------------Block Calculate Button logic------------------------

    self.getCost=function()
    {

    	self.body.selectedNumCores=self.selectedNumCores;
    	self.body.selected=self.selected;

		$log.info("Sending ",self.body);


		if(self.body.selected.length<=0)
		{
		    $mdDialog.show(
		    	      $mdDialog.alert()
		    	        .parent(angular.element(document.querySelector('#popupContainer')))
		    	        .clickOutsideToClose(true)
		    	        .title('Select at least one product!')
		    	        .ariaLabel('Alert Dialog')
		    	        .ok('OK')
		    	       )
		}
		else
		{

			if(self.body.modality=="SUBSCRIPTION"||self.body.modality=="ISV")
	    	{
			    var requestParams = {
			            method: 'POST',
			            url: '/knowageCalculator/rest/costInformation/calculateCostISVorSubscription',
			            headers: { 'Content-Type': 'application/json' },
			            data:	self.body,
			          };

			    $http(requestParams).then(function(evt)
			    	{
						$log.info("REST COMUNICATION OK, RECEIVED: ",evt);
				    	self.goldCost=evt.data.goldPrice.formatMoney(2,'.',',');
				    	self.silverCost=evt.data.silverPrice.formatMoney(2,'.',',');

						self.showSimpleResults=true;
			    	});
	    	}
			else if(self.body.modality=="OEM_EXT")
			{

			    var requestParams = {
			            method: 'POST',
			            url: '/knowageCalculator/rest/costInformation/calculateCostOEMext',
			            headers: { 'Content-Type': 'application/json' },
			            data:	self.body,
			          };

			    $http(requestParams).then(function(evt)
			    	{
						$log.info("REST COMUNICATION OK, RECEIVED: ",evt);  //add visualization tab logic
						self.categoryData=[];
						for(elementIdx in evt.data)
						{
							evt.data[elementIdx].silverPrice=evt.data[elementIdx].silverPrice.formatMoney(2,'.',',');
							evt.data[elementIdx].goldPrice=evt.data[elementIdx].goldPrice.formatMoney(2,'.',',');
							self.categoryData.push(evt.data[elementIdx]);
						}
						//self.categoryData=evt.data;
						self.showTableResults=true;
			    	});
			}
		}
    }

    if(self.body.modality=="OEM_INT")
    {
		self.showCalculate=false;

		self.body.selectedNumCores=self.selectedNumCores;
		self.body.selected=self.selected;

    	 var requestParams = {
		            method: 'POST',
		            url: '/knowageCalculator/rest/costInformation/calculateCostOEMint',
		            headers: { 'Content-Type': 'application/json' },
		            data:	self.body,
		          };

		    $http(requestParams).then(function(evt)
			    	{
						$log.info("REST COMUNICATION OK, RECEIVED: ",evt);  //add visualization tab logic
						self.categoryData=[];
						for(elementIdx in evt.data.silverTable)
						{
							var entry=evt.data.silverTable[elementIdx];
							entry.max_1_clients_price=entry.max_1_clients_price.formatMoney(2,'.',',')+" | "+entry.max_1_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_20_clients_price=entry.max_20_clients_price.formatMoney(2,'.',',')+" | "+entry.max_20_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_50_clients_price=entry.max_50_clients_price.formatMoney(2,'.',',')+" | "+entry.max_50_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_100_clients_price=entry.max_100_clients_price.formatMoney(2,'.',',')+" | "+entry.max_100_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_200_clients_price=entry.max_200_clients_price.formatMoney(2,'.',',')+" | "+entry.max_200_clients_maintenance_price.formatMoney(2,'.',',');
							entry.Unlimited_max_number_of_clients_price=entry.Unlimited_max_number_of_clients_price.formatMoney(2,'.',',')+" | "+entry.Unlimited_max_number_of_clients_maintenance_price.formatMoney(2,'.',',');

//							entry.max_1_clients_maintenance_price=entry.max_1_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_20_clients_maintenance_price=entry.max_20_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_50_clients_maintenance_price=entry.max_50_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_100_clients_maintenance_price=entry.max_100_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_200_clients_maintenance_price=entry.max_200_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.Unlimited_max_number_of_clients_maintenance_price=entry.Unlimited_max_number_of_clients_maintenance_price.formatMoney(2,'.',',');


							self.productsOEMintDataSilver.push(entry);
						}

						for(elementIdx in evt.data.goldTable)
						{
							var entry=evt.data.goldTable[elementIdx];
							entry.max_1_clients_price=entry.max_1_clients_price.formatMoney(2,'.',',')+" | "+entry.max_1_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_20_clients_price=entry.max_20_clients_price.formatMoney(2,'.',',')+" | "+entry.max_20_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_50_clients_price=entry.max_50_clients_price.formatMoney(2,'.',',')+" | "+entry.max_50_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_100_clients_price=entry.max_100_clients_price.formatMoney(2,'.',',')+" | "+entry.max_100_clients_maintenance_price.formatMoney(2,'.',',');
							entry.max_200_clients_price=entry.max_200_clients_price.formatMoney(2,'.',',')+" | "+entry.max_200_clients_maintenance_price.formatMoney(2,'.',',');
							entry.Unlimited_max_number_of_clients_price=entry.Unlimited_max_number_of_clients_price.formatMoney(2,'.',',')+" | "+entry.Unlimited_max_number_of_clients_maintenance_price.formatMoney(2,'.',',');

//							entry.max_1_clients_maintenance_price=entry.max_1_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_20_clients_maintenance_price=entry.max_20_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_50_clients_maintenance_price=entry.max_50_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_100_clients_maintenance_price=entry.max_100_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.max_200_clients_maintenance_price=entry.max_200_clients_maintenance_price.formatMoney(2,'.',',');
//							entry.Unlimited_max_number_of_clients_maintenance_price=entry.Unlimited_max_number_of_clients_maintenance_price.formatMoney(2,'.',',');

							self.productsOEMintDataGold.push(entry);
						}

						//self.categoryData=evt.data;
						self.showCalculate=false;
						self.showOEMintResults=true;
						self.productsOEMintDataResults=true;

						$log.info("Silver products saved: ",self.productsOEMintDataSilver);

			    	});
    }

}







