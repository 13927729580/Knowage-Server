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
package it.eng.spagobi.engines.qbe.services.core;

import it.eng.qbe.model.structure.IModelEntity;
import it.eng.qbe.model.structure.IModelStructure;
import it.eng.qbe.query.HavingField;
import it.eng.qbe.query.ISelectField;
import it.eng.qbe.query.Query;
import it.eng.qbe.query.SimpleSelectField;
import it.eng.qbe.query.WhereField;
import it.eng.qbe.serializer.SerializationException;
import it.eng.qbe.statement.AbstractQbeDataSet;
import it.eng.qbe.statement.IStatement;
import it.eng.qbe.statement.graph.GraphManager;
import it.eng.qbe.statement.graph.ModelFieldPaths;
import it.eng.qbe.statement.graph.QueryGraphBuilder;
import it.eng.qbe.statement.graph.bean.PathChoice;
import it.eng.qbe.statement.graph.bean.QueryGraph;
import it.eng.qbe.statement.graph.bean.Relationship;
import it.eng.qbe.statement.graph.bean.RootEntitiesGraph;
import it.eng.qbe.statement.graph.serializer.ModelFieldPathsJSONDeserializer;
import it.eng.qbe.statement.hibernate.HQLDataSet;
import it.eng.qbe.statement.jpa.JPQLDataSet;
import it.eng.spago.base.SourceBean;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.engines.qbe.QbeEngineConfig;
import it.eng.spagobi.engines.qbe.services.core.catalogue.SetCatalogueAction;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.common.datastore.DataStore;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.common.datastore.IField;
import it.eng.spagobi.tools.dataset.common.datastore.IRecord;
import it.eng.spagobi.tools.dataset.common.datastore.Record;
import it.eng.spagobi.tools.dataset.common.datawriter.JSONDataWriter;
import it.eng.spagobi.tools.dataset.common.query.AggregationFunctions;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.engines.EngineConstants;
import it.eng.spagobi.utilities.engines.SpagoBIEngineRuntimeException;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceException;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;
import it.eng.spagobi.utilities.service.JSONSuccess;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.log.Log;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * The Class ExecuteQueryAction.
 */
public class ExecuteQueryAction extends AbstractQbeEngineAction {

	private static final long serialVersionUID = -8812774864345259197L;

	// INPUT PARAMETERS
	public static final String LIMIT = "limit";
	public static final String START = "start";
	public static final String QUERY_ID = "id";
	public static final String AMBIGUOUS_FIELDS_PATHS = "ambiguousFieldsPaths";

	/** Logger component. */
	public static transient Logger logger = Logger.getLogger(ExecuteQueryAction.class);
	public static transient Logger auditlogger = Logger.getLogger("audit.query");

	@Override
	public void service(SourceBean request, SourceBean response) {
		// (Locale)getEngineInstance().getEnv().get(EngineConstants.ENV_LOCALE);

		// String queryId = null;
		Integer limit = null;
		Integer start = null;
		Integer maxSize = null;
		IDataStore dataStore = null;

		Query query = null;

		Integer resultNumber = null;
		JSONObject gridDataFeed = new JSONObject();

		Monitor totalTimeMonitor = null;
		Monitor errorHitsMonitor = null;

		logger.debug("IN");

		try {

			super.service(request, response);

			totalTimeMonitor = MonitorFactory.start("QbeEngine.executeQueryAction.totalTime");

			Object startO = getAttribute(START);
			if (startO != null && !startO.toString().equals("")) {
				start = getAttributeAsInteger(START);
			}
			logger.debug("Parameter [" + START + "] is equals to [" + start + "]");

			Object limitO = getAttribute(LIMIT);
			if (limitO != null && !limitO.toString().equals("")) {
				limit = getAttributeAsInteger(LIMIT);
			}


			Assert.assertNotNull(getEngineInstance(), "It's not possible to execute " + this.getActionName()
					+ " service before having properly created an instance of EngineInstance class");

			// retrieving query specified by id on request
			query = getQuery();
			Assert.assertNotNull(query, "Query object with id [" + getAttributeAsString(QUERY_ID) + "] does not exist in the catalogue");
			if (getEngineInstance().getActiveQuery() == null || !getEngineInstance().getActiveQuery().getId().equals(query.getId())) {
				logger.debug("Query with id [" + query.getId() + "] is not the current active query. A new statment will be generated");
				getEngineInstance().setActiveQuery(query);
			}

			// promptable filters values may come with request (read-only user modality)
			updatePromptableFiltersValue(query, this);

			Map<String, Map<String, String>> inlineFilteredSelectFields = query.getInlineFilteredSelectFields();

			boolean thereAreInlineTemporalFilters = inlineFilteredSelectFields != null && inlineFilteredSelectFields.size() > 0;
			if(thereAreInlineTemporalFilters) {
				limit = 0;
			}
			
			logger.debug("Parameter [" + LIMIT + "] is equals to [" + limit + "]");
			dataStore = executeQuery(start, limit);
			if(thereAreInlineTemporalFilters) {
				dataStore = handleTimeAggregations(dataStore);
			}
			
			resultNumber = (Integer) dataStore.getMetaData().getProperty("resultNumber");

			logger.debug("Total records: " + resultNumber);

			
			boolean overflow = maxSize != null && resultNumber >= maxSize;
			if (overflow) {
				logger.warn("Query results number [" + resultNumber + "] exceeds max result limit that is [" + maxSize + "]");
				// auditlogger.info("[" + userProfile.getUserId() + "]:: max result limit [" + maxSize + "] exceeded with SQL: " + sqlQuery);
			}

			gridDataFeed = serializeDataStore(dataStore);

			try {
				writeBackToClient(new JSONSuccess(gridDataFeed));
			} catch (IOException e) {
				String message = "Impossible to write back the responce to the client";
				throw new SpagoBIEngineServiceException(getActionName(), message, e);
			}

		} catch (Throwable t) {
			errorHitsMonitor = MonitorFactory.start("QbeEngine.errorHits");
			errorHitsMonitor.stop();
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException(getActionName(), getEngineInstance(), t);
		} finally {
			if (totalTimeMonitor != null)
				totalTimeMonitor.stop();
			logger.debug("OUT");
		}

	}



	private IDataStore handleTimeAggregations(IDataStore fullDatastore) {
		
		boolean debug = true;
		logger.debug("fullDatastore: ");
		sysoDatastore(fullDatastore);
		
		Query query = this.getQuery();
		Map<String, Map<String, String>> inlineFilteredSelectFields = query.getInlineFilteredSelectFields();
		if(inlineFilteredSelectFields != null && inlineFilteredSelectFields.size() > 0) {
			IDataStore finalDatastore = null;
			
			/*
			 * DATA FOR AGGREGATION
			 * */
			Set<String> aliasesToBeRemovedAfterExecution = query.getAliasesToBeRemovedAfterExecution();
			Map<String, String> hierarchyFullColumnMap = query.getHierarchyFullColumnMap();
			LinkedList<String> allYearsOnDWH = query.getAllYearsOnDWH();
			int relativeYearIndex = query.getRelativeYearIndex();
			Set<String> temporalFieldTypesInQuery = query.getTemporalFieldTypesInQuery();
			Map<String, List<String>> distinctPeriods = query.getDistinctPeriods();
			
			// riorganizzo i periodi per type
			Map<String, List<String>> distinctPeriodsByType = new LinkedHashMap<>();
			for (String type : hierarchyFullColumnMap.keySet()) {
				distinctPeriodsByType.put(type, distinctPeriods.get( hierarchyFullColumnMap.get(type)));	
			}
			
			/*
			 * END DATA FOR AGGREGATION
			 * */
			
			// elimino le groupby aggiuntive per ottenere tutte le righe della query finale
			List<ISelectField> selectFields = query.getSelectFields(false);
			for (ISelectField sfield : selectFields) {
				if (sfield.isSimpleField()) {
					SimpleSelectField ssField = (SimpleSelectField) sfield;
					if(aliasesToBeRemovedAfterExecution != null && aliasesToBeRemovedAfterExecution.contains(ssField.getUniqueName())) {
						ssField.setGroupByField(false);
						ssField.setFunction(AggregationFunctions.COUNT_FUNCTION);
					}
				}
			}
			
			// eseguo la query per avere il numero di righe finale
			finalDatastore = executeQuery(0, 0);
			

			logger.debug("finalDatastore: ");
			sysoDatastore(finalDatastore);
			
			// aggrego!
			for (Iterator finalIterator = finalDatastore.iterator(); finalIterator.hasNext();) {
				Record finalRecord = (Record) finalIterator.next();

				Map<String, String> rowPeriodValuesByType = new HashMap<>();
				for (int fieldIndex = 0; fieldIndex < finalDatastore.getMetaData().getFieldCount(); fieldIndex++) {
					String fieldName = finalDatastore.getMetaData().getFieldName(fieldIndex);
					if(fieldName != null && temporalFieldTypesInQuery.contains(fieldName)){
						rowPeriodValuesByType.put(fieldName, finalRecord.getFieldAt(fieldIndex).getValue().toString()); 
					}
				}
				
				
				// recupero l'identificativo della riga, rappresentato 
				// come coppie alias/valore
				Map<String, String> currentRecordId = getRecordAggregatedId(finalRecord, finalDatastore, query);
				
				Map<String, String> periodSetToCurrent = setCurrentIfNotPresent(query, hierarchyFullColumnMap, distinctPeriodsByType, currentRecordId);
				
				// Creo una mappa per tipo in cui tutti gli elementi sono numerati es i mesi da 0 a 11, i quarter da 0 a 3...
				Map<String, Integer> rowPeriodsNumbered = new HashMap<>();
				for (String type : rowPeriodValuesByType.keySet()) {
					String currentPeriodValue = rowPeriodValuesByType.get(type);
					
					if(periodSetToCurrent.get(type) != null) {
						currentPeriodValue = periodSetToCurrent.get(type);
					}
					
					List<String> distinctPeriodsForThisType = distinctPeriods.get(type);
					int currentValueIndexForThisType = -1;
					for(int i = 0; distinctPeriodsForThisType != null && i< distinctPeriodsForThisType.size(); i++) {
						String period = distinctPeriodsForThisType.get(i);
						if(period.equals(currentPeriodValue)) {
							currentValueIndexForThisType = i;
							break;
						}
					}
					rowPeriodsNumbered.put(type, currentValueIndexForThisType);	
				}
				
				
				String rowLog = "| ";
				
				// per ogni colonna di ogni riga, se c'è un operatore inline, ne calcolo il valore
				for (int fieldIndex = 0; fieldIndex < finalDatastore.getMetaData().getFieldCount(); fieldIndex++) {
					Map<String, String> firstRecordId = new HashMap<>();
					firstRecordId.putAll(currentRecordId);
					Map<String, String> lastRecordId = new HashMap<>();
					lastRecordId.putAll(currentRecordId);
					
					String fieldAlias = finalDatastore.getMetaData().getFieldAlias(fieldIndex);
					// se la colonna è da calcolare...
					if(fieldAlias != null && inlineFilteredSelectFields.containsKey(fieldAlias)){
						
						Map<String, String> inlineParameters = inlineFilteredSelectFields.get(fieldAlias);
						String temporalOperand = inlineParameters.get("temporalOperand");
						String temporalOperandParameter_str = inlineParameters.get("temporalOperandParameter");
						int temporalOperandParameter = Integer.parseInt(temporalOperandParameter_str);
						
						String periodType = null;
						boolean lastPeriod = false;
						switch (temporalOperand) {
						
						// PERIOD_TO_DATE
						// per i PERIOD_TO_DATE devo recuperare l'id temporale della riga  da cui partire, 
						// quella a cui fermarmi corrisponde con la riga corrente traslata nel periodo di riferimento
						// YTD_1 per la riga corrispondente a Giugno 2016 visualizzer� il dato aggregato da inizio 2015 a tutto Giugno 2015
						case SetCatalogueAction.TEMPORAL_OPERAND_YTD:
						{
							// PORTO AL PRIMO RECORD DEL ANNO
							for (String fieldType : temporalFieldTypesInQuery) {
								if(!hierarchyFullColumnMap.get("YEAR").equals(fieldType)) {
									firstRecordId.put(fieldType, distinctPeriods.get(fieldType).get(0));
								}
							}
							int parallelYearIndex = relativeYearIndex - temporalOperandParameter;
							if(parallelYearIndex >= 0 && allYearsOnDWH.size() > parallelYearIndex -1 ) {
								String parallelYear =  allYearsOnDWH.get(parallelYearIndex);
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), parallelYear);
								lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), parallelYear);
							}
							else {
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), null);
								lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), null);
							}
							break;
						}	
						case SetCatalogueAction.TEMPORAL_OPERAND_QTD:
							if (periodType == null) {
								periodType = "QUARTER";
							}
						case SetCatalogueAction.TEMPORAL_OPERAND_MTD:
							if (periodType == null) {
								periodType = "MONTH";
							}
						case SetCatalogueAction.TEMPORAL_OPERAND_WTD:
							if (periodType == null) {
								periodType = "WEEK";
							}
						case SetCatalogueAction.TEMPORAL_OPERAND_LAST_QUARTER:
							if (periodType == null) {
								periodType = "QUARTER";
								lastPeriod = true;
							}

						case SetCatalogueAction.TEMPORAL_OPERAND_LAST_MONTH:
							if (periodType == null) {
								periodType = "MONTH";
								lastPeriod = true;
							}

						case SetCatalogueAction.TEMPORAL_OPERAND_LAST_WEEK:
							if (periodType == null) {
								periodType = "WEEK";
								lastPeriod = true;
							}
						{
							// PORTO AL PRIMO RECORD DEL PERIODO (nell'anno)
							for (String fieldType : temporalFieldTypesInQuery) {
								if(!hierarchyFullColumnMap.get("YEAR").equals(fieldType) &&
								   !hierarchyFullColumnMap.get(periodType).equals(fieldType)) {
									firstRecordId.put(fieldType, distinctPeriods.get(fieldType).get(0));
								}
							}
							
							Integer rowPeriodNumber = rowPeriodsNumbered.get(hierarchyFullColumnMap.get(periodType));
							rowPeriodNumber = rowPeriodNumber > 0 ? rowPeriodNumber : 0;
							Integer otherPeriodNumber = rowPeriodNumber - temporalOperandParameter;
							
							
							/*
							if(otherPeriodNumber < rowPeriodNumber) {
								otherPeriodNumber = otherPeriodNumber + 1;
							}
							else {
								otherPeriodNumber = otherPeriodNumber - 1;
							}
							*/
							
							List<String> periods = distinctPeriodsByType.get(periodType);
							int periodsCount = periods.size();
							int periodOtherIndex = (otherPeriodNumber % periodsCount);
							
							int yearOffset = 0;
							while (periodOtherIndex < 0) {
								periodOtherIndex += periodsCount;
								yearOffset--;
							}
							while (periodOtherIndex >= periodsCount) {
								periodOtherIndex = periodOtherIndex % periodsCount;
								yearOffset++;
							}
							
							int yearOtherIndex = (int) (relativeYearIndex + yearOffset);
							if(yearOtherIndex < 0) {
								yearOtherIndex = 0;
							}
							if(yearOtherIndex >= allYearsOnDWH.size()) {
								yearOtherIndex = allYearsOnDWH.size() -1;
								periodOtherIndex = periods.size() -1;
							}
							// L'ANNO LO DEVO METTERE SOLO SE PRESENTE TRA I CAMPI DELLA SELECT ???
							firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.get(yearOtherIndex));
							firstRecordId.put(hierarchyFullColumnMap.get(periodType), periods.get(periodOtherIndex));
							
							if(lastPeriod) {
								// se operatore last, aggrego fino al periodo della riga corrente
								lastRecordId.put(hierarchyFullColumnMap.get(periodType), rowPeriodValuesByType.get(hierarchyFullColumnMap.get(periodType)));
								lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.get(relativeYearIndex));
							}
							else {
								// se operatore period to date, aggrego fino allo stesso 'tempo' nel periodo di riferimento
								lastRecordId.put(hierarchyFullColumnMap.get(periodType), rowPeriodValuesByType.get(hierarchyFullColumnMap.get(periodType)));
								lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.get(relativeYearIndex));
							}
							break;
						}
							
							
						// LAST_PERIOD
						// per i LAST_PERIOD devo recuperare l'id temporale della riga da cui partire, 
						// quella a cui fermarmi corrisponde con la riga corrente
						// LM_3 per la riga Giugno 2016 visualizzer� il dato aggregato da Aprile a Giugno 2015
						// LM_4 per la riga Gennaio 2016 visualizzer� il dato aggregato da Ottobre 2015 a Gennaio 2016
						case SetCatalogueAction.TEMPORAL_OPERAND_LAST_YEAR:
						{
							// setta gennaio/Q1/W1
							for (String fieldType : temporalFieldTypesInQuery) {
								if(!hierarchyFullColumnMap.get("YEAR").equals(fieldType)) {
									firstRecordId.put(fieldType, distinctPeriods.get(fieldType).get(0));
								}
							}
							
							int parallelYearIndex = relativeYearIndex - temporalOperandParameter;
							if(parallelYearIndex >= 0 && allYearsOnDWH.size() > parallelYearIndex ) {
								String parallelYear =  allYearsOnDWH.get(parallelYearIndex);
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), parallelYear);
							}
							else if(parallelYearIndex < 0) {
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.getFirst());
							}
							else {
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.getLast());
							}
							
							if(relativeYearIndex >= 0 && allYearsOnDWH.size() > relativeYearIndex ) {
								lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.get(relativeYearIndex));
							}
							else if(relativeYearIndex < 0) {
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.getFirst());
							}
							else {
								firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), allYearsOnDWH.getLast());
							}
							
							break;
						}

						// PARALLEL_PERIOD
						case SetCatalogueAction.TEMPORAL_OPERAND_PARALLEL_YEAR:
						{
							// i parallel years si calcolano sempre in funzione di quello che trovo nella where
							
							String year = null;
							
							int parallelYearIndex = relativeYearIndex - temporalOperandParameter;
							if(parallelYearIndex >= 0 && allYearsOnDWH.size() > parallelYearIndex ) {
								year =  allYearsOnDWH.get(parallelYearIndex);
							}
							firstRecordId.put(hierarchyFullColumnMap.get("YEAR"), year);
							lastRecordId.put(hierarchyFullColumnMap.get("YEAR"), year);
							break;
						}
						default:
							break;
						}
						
						
						setCurrentIfNotPresent(query, hierarchyFullColumnMap, distinctPeriodsByType, firstRecordId);
						setCurrentIfNotPresent(query, hierarchyFullColumnMap, distinctPeriodsByType, lastRecordId);
						
						int firstRecordIndex = calculateRecordIndex(hierarchyFullColumnMap, distinctPeriodsByType, firstRecordId);
						int lastRecordIndex = calculateRecordIndex(hierarchyFullColumnMap, distinctPeriodsByType, lastRecordId);
						
						boolean swapped = false;
						if(firstRecordIndex > lastRecordIndex) {
							int swap = lastRecordIndex;
							lastRecordIndex = firstRecordIndex;
							firstRecordIndex = swap;
						}
						logger.debug( fieldAlias +" FIRST: "+firstRecordIndex + " -> LAST: " + lastRecordIndex + (swapped?" (Reading the future: swapped first and last!)":""));

						
						/** A QUESTO PUNTO AGGREGO E CALCOLO IL VALORE */
						if(firstRecordId.get(hierarchyFullColumnMap.get("YEAR")) != null) {
							double finalValue = 0D;
							boolean aValueFound = false;
							/** INQUESTO CICLO DEVO UTILIZZARE I CAMPI FIRST E LAST */
							for (Iterator fullIterator = fullDatastore.iterator(); fullIterator.hasNext();) {
								Record record = (Record) fullIterator.next();
								Map<String, String> recordId = getRecordFullId(record, finalDatastore, query);
								
								int recordIndex = calculateRecordIndex(hierarchyFullColumnMap, distinctPeriodsByType, recordId);
								
								
								if(firstRecordIndex <= recordIndex && recordIndex <= lastRecordIndex) {
									logger.debug("recordIndex: " + recordIndex);
									aValueFound = true;
									finalValue += Double.parseDouble(record.getFieldAt(fieldIndex).getValue().toString());
									finalRecord.getFieldAt(fieldIndex).setValue(finalValue);
								}
							}
							if(!aValueFound) {
								finalRecord.getFieldAt(fieldIndex).setValue(0D);
							}
						}
						else {
							finalRecord.getFieldAt(fieldIndex).setValue(0D);
						}
						
						rowLog += " | " + firstRecordId + " >>> " + lastRecordId;
					}
					else {
						rowLog += " | NON AGGREGATO ";
					}
				}
				
				logger.debug(rowLog);
				
			}
			
			return finalDatastore;
			
		}
		else {
			return fullDatastore;
		}
		
		
	}



	public Map<String,String> setCurrentIfNotPresent(Query query, Map<String, String> hierarchyFullColumnMap,
			Map<String, List<String>> distinctPeriodsByType, Map<String, String> currentRecordId) {
		Map<String,String> periodSetToCurrent = new HashMap<>();
		Set<String> periodElements = distinctPeriodsByType.keySet();
		for (String period : periodElements) {
			List<String> periods = distinctPeriodsByType.get(period);
			if(periods != null){
				String periodUniqueIdentifier = hierarchyFullColumnMap.get(period);
				String currentRecordPeriod = currentRecordId.get(periodUniqueIdentifier);
				if(currentRecordPeriod != null) {
					int periodIndex = periods.indexOf(currentRecordPeriod);
					if(periodIndex < 0) {
						currentRecordId.put(periodUniqueIdentifier, query.getCurrentPeriodValuyesByType().get(periodUniqueIdentifier));
						periodSetToCurrent.put(periodUniqueIdentifier, query.getCurrentPeriodValuyesByType().get(periodUniqueIdentifier));
					}
				}
			}
		}
		return periodSetToCurrent;
	}



	public int calculateRecordIndex(Map<String, String> hierarchyFullColumnMap,
			Map<String, List<String>> distinctPeriodsByType, Map<String, String> recordId)
					throws NumberFormatException {
		String recordCode = "";
		Set<String> periodElements = distinctPeriodsByType.keySet();
		for (String period : periodElements) {
			List<String> periods = distinctPeriodsByType.get(period);
			if(periods != null){
				int periodIndex = periods.indexOf(recordId.get(hierarchyFullColumnMap.get(period)));
				recordCode += new DecimalFormat("000").format(periodIndex+1);
			}
		}
		int recordIndex = new Integer(recordCode.indexOf('-') < 0 ? recordCode : "0");
		return recordIndex;
	}



	public void sysoDatastore(IDataStore ds) throws RuntimeException {
		try {
		JSONDataWriter dataSetWriter = new JSONDataWriter();
		JSONObject dataSetJSON = (JSONObject) dataSetWriter.write(ds);
			logger.debug(dataSetJSON.getJSONArray("rows").toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, String> getRecordAggregatedId(Record finalRecord, IDataStore finalDatastore, Query query) {
		Set<String> idAliases = query.getTemporalFieldTypesInSelect();
		return getRecordId(finalRecord, finalDatastore, query, idAliases);
	}
	 
	private Map<String, String> getRecordFullId(Record finalRecord, IDataStore finalDatastore, Query query) {
		Set<String> idAliases = query.getTemporalFieldTypesInQuery();
		return getRecordId(finalRecord, finalDatastore, query, idAliases);
	}
	
	private Map<String, String> getRecordId(Record finalRecord, IDataStore finalDatastore, Query query, Set<String> idAliases) {
		Map<String, String> recordId = new LinkedHashMap<>();
		for (int fieldIndex = 0; fieldIndex < finalDatastore.getMetaData().getFieldCount(); fieldIndex++) {
			String fieldName = finalDatastore.getMetaData().getFieldName(fieldIndex);
			if(fieldName != null && idAliases.contains(fieldName)){
				recordId.put(fieldName, (finalRecord.getFieldAt(fieldIndex).getValue() != null ? finalRecord.getFieldAt(fieldIndex).getValue().toString(): "") );
			}
		}
		return recordId;
	}

	public static List<ModelFieldPaths> deserializeList(String serialized, Collection<Relationship> relationShips, IModelStructure modelStructure, Query query)
			throws SerializationException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule simpleModule = new SimpleModule("SimpleModule", new Version(1, 0, 0, null));
		simpleModule.addDeserializer(ModelFieldPaths.class, new ModelFieldPathsJSONDeserializer(relationShips, modelStructure, query));
		mapper.registerModule(simpleModule);
		TypeReference<List<ModelFieldPaths>> type = new TypeReference<List<ModelFieldPaths>>() {
		};
		try {
			return mapper.readValue(serialized, type);
		} catch (Exception e) {
			throw new SerializationException("Error deserializing the list of ModelFieldPaths", e);
		}
	}

	protected IStatement getStatement(Query query) {
		IStatement statement = getDataSource().createStatement(query);
		return statement;
	}

	public JSONObject serializeDataStore(IDataStore dataStore) {
		JSONDataWriter dataSetWriter = new JSONDataWriter();
		JSONObject gridDataFeed = (JSONObject) dataSetWriter.write(dataStore);
		return gridDataFeed;
	}

	/**
	 * Get the query id from the request
	 *
	 * @return
	 */
	@Override
	public Query getQuery() {
		String queryId = getAttributeAsString(QUERY_ID);
		logger.debug("Parameter [" + QUERY_ID + "] is equals to [" + queryId + "]");
		Query query = getEngineInstance().getQueryCatalogue().getQuery(queryId);
		return query;
	}

	public static void updatePromptableFiltersValue(Query query, AbstractQbeEngineAction action) throws JSONException {
		updatePromptableFiltersValue(query, action, false);
	}

	public static void updatePromptableFiltersValue(Query query, AbstractQbeEngineAction action, boolean useDefault) throws JSONException {
		logger.debug("IN");
		List whereFields = query.getWhereFields();
		Iterator whereFieldsIt = whereFields.iterator();
		String[] question = { "?" };

		JSONObject requestPromptableFilters = action.getAttributeAsJSONObject("promptableFilters");

		while (whereFieldsIt.hasNext()) {
			WhereField whereField = (WhereField) whereFieldsIt.next();
			if (whereField.isPromptable()) {
				// getting filter value on request
				if (!useDefault || requestPromptableFilters != null) {
					JSONArray promptValuesList = requestPromptableFilters.optJSONArray(whereField.getName());
					if (promptValuesList != null) {
						String[] promptValues = toStringArray(promptValuesList);
						logger.debug("Read prompts " + promptValues + " for promptable filter " + whereField.getName() + ".");
						whereField.getRightOperand().lastValues = promptValues;
					}
				} else {
					whereField.getRightOperand().lastValues = question;
				}
			}
		}
		List havingFields = query.getHavingFields();
		Iterator havingFieldsIt = havingFields.iterator();
		while (havingFieldsIt.hasNext()) {
			HavingField havingField = (HavingField) havingFieldsIt.next();
			if (havingField.isPromptable()) {
				if (!useDefault || requestPromptableFilters != null) {
					// getting filter value on request
					// promptValuesList = action.getAttributeAsList(havingField.getEscapedName());
					JSONArray promptValuesList = requestPromptableFilters.optJSONArray(havingField.getName());
					if (promptValuesList != null) {
						String[] promptValues = toStringArray(promptValuesList);
						logger.debug("Read prompt value " + promptValues + " for promptable filter " + havingField.getName() + ".");
						havingField.getRightOperand().lastValues = promptValues; // TODO how to manage multi-values prompts?
					}
				}
			} else {
				havingField.getRightOperand().lastValues = question;
			}
		}
		logger.debug("OUT");
	}

	private static String[] toStringArray(JSONArray o) throws JSONException {
		String[] promptValues = new String[o.length()];
		for (int i = 0; i < o.length(); i++) {
			promptValues[i] = o.getString(i);
		}
		return promptValues;
	}

	public IDataStore executeQuery(Integer start, Integer limit) {
		IDataStore dataStore = null;
		IDataSet dataSet = this.getEngineInstance().getActiveQueryAsDataSet();
		AbstractQbeDataSet qbeDataSet = (AbstractQbeDataSet) dataSet;
		IStatement statement = qbeDataSet.getStatement();
		QueryGraph graph = statement.getQuery().getQueryGraph();
		boolean valid = GraphManager.getGraphValidatorInstance(QbeEngineConfig.getInstance().getGraphValidatorImpl()).isValid(graph,
				statement.getQuery().getQueryEntities(getDataSource()));
		logger.debug("QueryGraph valid = " + valid);
		if (!valid) {
			throw new SpagoBIEngineServiceException(getActionName(), "error.mesage.description.relationship.not.enough");
		}
		try {
			logger.debug("Executing query ...");
			Integer maxSize = QbeEngineConfig.getInstance().getResultLimit();
			logger.debug("Configuration setting  [" + "QBE.QBE-SQL-RESULT-LIMIT.value" + "] is equals to [" + (maxSize != null ? maxSize : "none") + "]");
			String jpaQueryStr = statement.getQueryString();
			
			logger.debug("Executable query (HQL/JPQL): [" + jpaQueryStr + "]");

			logQueryInAudit(qbeDataSet);

			dataSet.loadData(start, limit, (maxSize == null ? -1 : maxSize.intValue()));
			dataStore = dataSet.getDataStore();
			Assert.assertNotNull(dataStore, "The dataStore returned by loadData method of the class [" + dataSet.getClass().getName() + "] cannot be null");
		} catch (Exception e) {
			logger.debug("Query execution aborted because of an internal exceptian");
			SpagoBIEngineServiceException exception;
			String message;

			message = "An error occurred in " + getActionName() + " service while executing query: [" + statement.getQueryString() + "]";
			exception = new SpagoBIEngineServiceException(getActionName(), message, e);
			exception.addHint("Check if the query is properly formed: [" + statement.getQueryString() + "]");
			exception.addHint("Check connection configuration");
			exception.addHint("Check the qbe jar file");

			throw exception;
		}
		logger.debug("Query executed succesfully");
		return dataStore;
	}

	private void logQueryInAudit(AbstractQbeDataSet dataset) {
		UserProfile userProfile = (UserProfile) getEnv().get(EngineConstants.ENV_USER_PROFILE);

		if (dataset instanceof JPQLDataSet) {
			auditlogger.info("[" + userProfile.getUserId() + "]:: JPQL: " + dataset.getStatement().getQueryString());
			auditlogger.info("[" + userProfile.getUserId() + "]:: SQL: " + ((JPQLDataSet) dataset).getSQLQuery(true));
		} else if (dataset instanceof HQLDataSet) {
			auditlogger.info("[" + userProfile.getUserId() + "]:: HQL: " + dataset.getStatement().getQueryString());
			auditlogger.info("[" + userProfile.getUserId() + "]:: SQL: " + ((HQLDataSet) dataset).getSQLQuery(true));
		} else {
			auditlogger.info("[" + userProfile.getUserId() + "]:: SQL: " + dataset.getStatement().getSqlQueryString());
		}

	}

}
