
var entities = angular.module('entities',['sbiModule']);

entities.service('entity_service',function(sbiModule_action){
	
	this.getEntitiyTree = function(datamartName){
		var queryParam = {};
	
		queryParam.DATAMART_NAME = datamartName;
		
		return sbiModule_action.promiseGet('GET_TREE_ACTION',queryParam,null);
		
	}
})