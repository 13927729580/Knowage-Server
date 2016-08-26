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

package it.eng.spagobi.engines.whatif.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.OlapException;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;
import org.pivot4j.PivotModel;

import it.eng.spagobi.engines.whatif.WhatIfEngineInstance;
import it.eng.spagobi.engines.whatif.calculatedmember.CalculatedMember;
import it.eng.spagobi.engines.whatif.common.AbstractWhatIfEngineService;
import it.eng.spagobi.engines.whatif.cube.CubeUtilities;
import it.eng.spagobi.engines.whatif.dimension.SbiDimension;
import it.eng.spagobi.engines.whatif.hierarchy.SbiHierarchy;
import it.eng.spagobi.engines.whatif.model.ModelConfig;
import it.eng.spagobi.engines.whatif.model.SpagoBIPivotModel;
import it.eng.spagobi.utilities.engines.SpagoBIEngineException;
import it.eng.spagobi.utilities.exceptions.SpagoBIEngineRestServiceRuntimeException;
import it.eng.spagobi.utilities.rest.RestUtilities;

@Path("/1.0/calculatedmembers")
public class CalculatedMembersResource extends AbstractWhatIfEngineService {
	public static transient Logger logger = Logger.getLogger(CalculatedMembersResource.class);
	public static final String DIVISION_SIGN = new String("spagobi.operator.division");

	/**
	 * Service to get Dimensions
	 *
	 * @return the dimensions
	 */
	@GET
	@Path("/initializeData")
	@Produces("text/html; charset=UTF-8")
	public String initializeData() {
		logger.debug("IN");
		WhatIfEngineInstance ei = getWhatIfEngineInstance();
		PivotModel model = ei.getPivotModel();
		List<SbiDimension> dimensions = new ArrayList<SbiDimension>();
		String serializedNames = new String();
		try {
			dimensions = getDimensions(model, ei.getModelConfig());
			serializedNames = serialize(dimensions);
		} catch (Exception e) {
			logger.error("Error serializing dimensions");
			throw new SpagoBIEngineRestServiceRuntimeException(getLocale(), e);
		}
		logger.debug("OUT");
		return serializedNames;
	}

	/**
	 * Service to get the dimensions
	 *
	 * @return The SbiDimension List
	 */
	public List<SbiDimension> getDimensions(PivotModel model, ModelConfig modelConfig) throws SpagoBIEngineException {
		logger.debug("IN");
		CellSet cellSet = model.getCellSet();
		List<CellSetAxis> axis = cellSet.getAxes();
		List<Dimension> otherHDimensions;
		List<SbiDimension> dimensions = new ArrayList<SbiDimension>();
		try {
			List<Hierarchy> axisHierarchies = axis.get(0).getAxisMetaData().getHierarchies();
			axisHierarchies.addAll(axis.get(1).getAxisMetaData().getHierarchies());
			otherHDimensions = CubeUtilities.getDimensions(model.getCube().getHierarchies());
			for (int i = 0; i < otherHDimensions.size(); i++) {
				Dimension aDimension = otherHDimensions.get(i);
				SbiDimension myDimension = new SbiDimension(aDimension, -1, i);
				List<Hierarchy> dimensionHierarchies = aDimension.getHierarchies();
				String selectedHierarchyName = modelConfig.getDimensionHierarchyMap().get(myDimension.getUniqueName());
				if (selectedHierarchyName == null) {
					selectedHierarchyName = aDimension.getDefaultHierarchy().getUniqueName();
				}
				myDimension.setSelectedHierarchyUniqueName(selectedHierarchyName);
				for (int j = 0; j < dimensionHierarchies.size(); j++) {
					Hierarchy hierarchy = dimensionHierarchies.get(j);
					SbiHierarchy hierarchyObject = new SbiHierarchy(hierarchy, i);
					myDimension.getHierarchies().add(hierarchyObject);
					// set the position of the selected hierarchy
					if (selectedHierarchyName.equals(hierarchy.getUniqueName())) {
						myDimension.setSelectedHierarchyPosition(j);
					}
				}
				dimensions.add(myDimension);
			}
		} catch (Exception e) {
			logger.error("Error getting dimensions", e);
			throw new SpagoBIEngineException("Error getting dimensions", e);
		}
		logger.debug("OUT");
		return dimensions;
	}

	/**
	 * Service to create the calculated member
	 *
	 * @return the rendered pivot table
	 */
	@POST
	@Path("/")
	// execute/{calculateFieldName}/{calculateFieldFormula}/{parentMemberUniqueName}/{axisOrdinal}
	@Produces("text/html; charset=UTF-8")
	public String execute(@javax.ws.rs.core.Context HttpServletRequest req) {
		logger.debug("IN");
		Member parentMember = null;
		JSONObject jo;
		String calculatedFieldName = null;
		String calculatedFieldFormula = null;
		String parentMemberUniqueName = null;
		String hierarchyUniqueName = null;
		int axisOrdinal = 0;
		WhatIfEngineInstance ei = getWhatIfEngineInstance();

		try {
			jo = RestUtilities.readBodyAsJSONObject(req);
			calculatedFieldName = jo.getString("calculatedFieldName");
			calculatedFieldFormula = jo.getString("calculatedFieldFormula");
			parentMemberUniqueName = jo.getString("parentMemberUniqueName");
			axisOrdinal = jo.getInt("axisOrdinal");
			hierarchyUniqueName = jo.getString("hierarchyUniqueName");

		} catch (IOException e1) { // TODO Auto-generated catch block
			logger.error("Error reading body", e1);
		} catch (JSONException e) {
			logger.error("Error making JSON", e);
		}

		logger.debug("expression= " + calculatedFieldFormula);

		Axis axis;
		String calculateFieldFormulaParsed = new String();
		Hierarchy hierarchy;
		try {
			if (!calculatedFieldFormula.isEmpty()) {
				calculateFieldFormulaParsed = calculatedFieldFormula.replaceAll("\\{" + DIVISION_SIGN + "\\}", "/");
			}

		} catch (Exception e) {
			logger.error("Error parsing the formula. The original formula is " + calculatedFieldFormula, e);
		}
		try {

			parentMember = CubeUtilities.getMember(ei.getPivotModel().getCube(), parentMemberUniqueName);

			axis = CubeUtilities.getAxis(axisOrdinal);
			hierarchy = CubeUtilities.getHierarchy(ei.getPivotModel().getCube(), hierarchyUniqueName);
		} catch (OlapException e) {
			logger.error("Error getting the parent of the calculated field. The unique name of the parent is " + parentMemberUniqueName, e);
			throw new SpagoBIEngineRestServiceRuntimeException("sbi.olap.celculated.definition.error", getLocale(),
					"Error getting the parent of the calculated field. The unique name of the parent is " + parentMemberUniqueName, e);
		}

		logger.debug("Adding the calculated fields into the model");
		CalculatedMember cc = new CalculatedMember(calculatedFieldName, calculateFieldFormulaParsed, parentMember, axis);

		ei.getSpagoBIPivotModel().addCalculatedField(cc);

		String table = renderModel(ei.getPivotModel());
		logger.debug("OUT");
		return table;
	}

	/**
	 * Service to delete the calculated member
	 *
	 * @return the rendered pivot table
	 */
	@POST
	@Path("/delete")
	// {calculatedFieldName}
	@Produces("text/html; charset=UTF-8")
	public String deleteCalculatedMember(@javax.ws.rs.core.Context HttpServletRequest req) {
		logger.debug("IN");
		WhatIfEngineInstance ei = getWhatIfEngineInstance();
		SpagoBIPivotModel model = ei.getSpagoBIPivotModel();
		String calculatedFieldName = null;
		JSONObject jo;
		try {
			jo = RestUtilities.readBodyAsJSONObject(req);
			calculatedFieldName = jo.getString("calculatedFieldName");

		} catch (IOException e1) {
			logger.error("Error reading body", e1);
		} catch (JSONException e) {
			logger.error("Error making JSON", e);
		}
		logger.debug("Deleting of calculated member with name" + calculatedFieldName);
		if (model.removeCalculatedField(calculatedFieldName)) {
			logger.debug("Member" + calculatedFieldName + " deleted");
		} else {
			logger.debug("There is no member with name" + calculatedFieldName);
		}

		String table = renderModel(model);
		logger.debug("OUT");
		return table;
	}

}
