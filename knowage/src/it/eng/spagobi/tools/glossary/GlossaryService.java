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
package it.eng.spagobi.tools.glossary;

import static it.eng.spagobi.tools.glossary.util.Util.fromBnessClsLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromContent;
import static it.eng.spagobi.tools.glossary.util.Util.fromContentsLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromDataSetLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromDocumentLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromGlossary;
import static it.eng.spagobi.tools.glossary.util.Util.fromGlossaryLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromTableLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromUdpLight;
import static it.eng.spagobi.tools.glossary.util.Util.fromWord;
import static it.eng.spagobi.tools.glossary.util.Util.fromWordLight;
import static it.eng.spagobi.tools.glossary.util.Util.getNumberOrNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.util.JSON;

import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.analiticalmodel.document.metadata.SbiObjects;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.metadata.metadata.SbiMetaBc;
import it.eng.spagobi.metadata.metadata.SbiMetaBcAttribute;
import it.eng.spagobi.metadata.metadata.SbiMetaSource;
import it.eng.spagobi.metadata.metadata.SbiMetaTable;
import it.eng.spagobi.metadata.metadata.SbiMetaTableColumn;
import it.eng.spagobi.services.rest.annotations.ManageAuthorization;
import it.eng.spagobi.services.rest.annotations.UserConstraint;
import it.eng.spagobi.services.serialization.JsonConverter;
import it.eng.spagobi.tools.dataset.common.metadata.MetaData;
import it.eng.spagobi.tools.dataset.metadata.SbiDataSet;
import it.eng.spagobi.tools.glossary.dao.IGlossaryDAO;
import it.eng.spagobi.tools.glossary.metadata.SbiGlBnessClsWlist;
import it.eng.spagobi.tools.glossary.metadata.SbiGlBnessClsWlistId;
import it.eng.spagobi.tools.glossary.metadata.SbiGlContents;
import it.eng.spagobi.tools.glossary.metadata.SbiGlDataSetWlist;
import it.eng.spagobi.tools.glossary.metadata.SbiGlDataSetWlistId;
import it.eng.spagobi.tools.glossary.metadata.SbiGlDocWlist;
import it.eng.spagobi.tools.glossary.metadata.SbiGlDocWlistId;
import it.eng.spagobi.tools.glossary.metadata.SbiGlGlossary;
import it.eng.spagobi.tools.glossary.metadata.SbiGlTableWlist;
import it.eng.spagobi.tools.glossary.metadata.SbiGlTableWlistId;
import it.eng.spagobi.tools.glossary.metadata.SbiGlWord;
import it.eng.spagobi.tools.udp.bo.Udp;
import it.eng.spagobi.tools.udp.dao.IUdpDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;
import it.eng.spagobi.utilities.rest.RestUtilities;

/**
 * @authors Giovanni Luca Ulivo (GiovanniLuca.Ulivo@eng.it)
 *
 */
@Path("/1.0/glossary")
@ManageAuthorization
public class GlossaryService {
	final String DirectDSWordChild = ".SELF";

	static protected Logger logger = Logger.getLogger(GlossaryService.class);

	@POST
	@Path("/addDocWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String addDocWlist(@Context HttpServletRequest req) {
		JSONObject jo = new JSONObject();
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			JSONObject requestVal = RestUtilities.readBodyAsJSONObject(req);
			Integer wordId = getNumberOrNull(requestVal.opt("WORD_ID"));
			Integer documentId = getNumberOrNull(requestVal.opt("DOCUMENT_ID"));

			SbiGlDocWlist docwl = new SbiGlDocWlist();
			SbiGlDocWlistId dwlid = new SbiGlDocWlistId(wordId, documentId);
			SbiGlDocWlist wo = dao.getDocWlistOrNull(dwlid);

			if (wo != null) {
				jo.put("Status", "NON OK");
				jo.put("Message", "sbi.glossary.word.new.name.duplicate");
				return jo.toString();
			}

			docwl.setId(dwlid);
			dao.insertDocWlist(docwl);

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
		return jo.toString();
	}

	@POST
	@Path("/addDataSetWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String addDataSetWlist(@Context HttpServletRequest req) {
		JSONObject jo = new JSONObject();
		try {
			logger.debug("addDataSetWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			JSONObject requestVal = RestUtilities.readBodyAsJSONObject(req);
			Integer wordId = getNumberOrNull(requestVal.opt("WORD_ID"));
			Integer datasetId = getNumberOrNull(requestVal.opt("DATASET_ID"));
			String column = requestVal.opt("COLUMN_NAME").toString();
			String organization = requestVal.opt("ORGANIZATION").toString();

			SbiGlDataSetWlist dswl = new SbiGlDataSetWlist();
			SbiGlDataSetWlistId dslid = new SbiGlDataSetWlistId(wordId, datasetId, organization, column);
			SbiGlDataSetWlist wo = dao.getDataSetWlistOrNull(dslid);

			if (wo != null) {
				jo.put("Status", "NON OK");
				jo.put("Message", "sbi.glossary.word.new.name.duplicate");
				return jo.toString();
			}

			dswl.setId(dslid);
			dao.insertDataSetWlist(dswl);

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
		return jo.toString();
	}

	@POST
	@Path("/addMetaTableWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public Response addMetaTableWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("addMetaTableWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			JSONObject requestVal = RestUtilities.readBodyAsJSONObject(req);
			Integer wordId = getNumberOrNull(requestVal.opt("WORD_ID"));
			Integer metaTableId = getNumberOrNull(requestVal.opt("META_TABLE_ID"));
			String column = requestVal.opt("COLUMN_NAME").toString();
			// String organization = requestVal.opt("ORGANIZATION").toString();

			SbiGlTableWlist twl = new SbiGlTableWlist();
			SbiGlTableWlistId twlId = new SbiGlTableWlistId(wordId, metaTableId, column);
			SbiGlTableWlist wo = dao.getTableWlistOrNull(twlId);
			if (wo != null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "sbi.glossary.word.new.name.duplicate");
			}

			twl.setId(twlId);
			dao.insertTableWlist(twl);

		} catch (Throwable t) {
			t.printStackTrace();
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
		return Response.ok().build();
	}

	@POST
	@Path("/addMetaBcWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public Response addMetaBcWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("addMetaTableWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			JSONObject requestVal = RestUtilities.readBodyAsJSONObject(req);
			Integer wordId = getNumberOrNull(requestVal.opt("WORD_ID"));
			Integer metaBcId = getNumberOrNull(requestVal.opt("META_BC_ID"));
			String column = requestVal.opt("COLUMN_NAME").toString();
			// String organization = requestVal.opt("ORGANIZATION").toString();

			SbiGlBnessClsWlist twl = new SbiGlBnessClsWlist();
			SbiGlBnessClsWlistId twlId = new SbiGlBnessClsWlistId(wordId, metaBcId, column);
			SbiGlBnessClsWlist wo = dao.getBcWlistOrNull(twlId);
			if (wo != null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "sbi.glossary.word.new.name.duplicate");
			}

			twl.setId(twlId);
			dao.insertBcWlist(twl);

		} catch (Throwable t) {
			t.printStackTrace();
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
		return Response.ok().build();
	}

	@POST
	@Path("/deleteDocWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String deleteDocWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("deleteDocWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			Integer wordId = getNumberOrNull(req.getParameter("WORD_ID"));
			Integer documentId = getNumberOrNull(req.getParameter("DOCUMENT_ID"));

			SbiGlDocWlistId id = new SbiGlDocWlistId(wordId, documentId);

			dao.deleteDocWlist(id);

			JSONObject jo = new JSONObject();
			jo.put("Status", "OK");
			return jo.toString();

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@POST
	@Path("/deleteDatasetWlist")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String deleteDatasetWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("deleteDatasetWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			Integer wordId = getNumberOrNull(req.getParameter("WORD_ID"));
			Integer datasetId = getNumberOrNull(req.getParameter("DATASET_ID"));
			String column = req.getParameter("COLUMN").toString();
			String organiz = req.getParameter("ORGANIZATION").toString();

			SbiGlDataSetWlistId id = new SbiGlDataSetWlistId(wordId, datasetId, organiz, column);

			dao.deleteDataSetWlist(id);

			JSONObject jo = new JSONObject();
			jo.put("Status", "OK");
			return jo.toString();

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@DELETE
	@Path("/deleteMetaTableWlist")
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public Response deleteMetaTableWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("deleteMetaTableWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			Integer wordId = getNumberOrNull(req.getParameter("WORD_ID"));
			Integer tableId = getNumberOrNull(req.getParameter("TABLE_ID"));
			String column = req.getParameter("COLUMN").toString();
			SbiGlTableWlistId id = new SbiGlTableWlistId(wordId, tableId, column);

			dao.deleteMetaTableWlist(id);

			JSONObject jo = new JSONObject();
			jo.put("Status", "OK");
			return Response.ok().build();

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@DELETE
	@Path("/deleteMetaBcWlist")
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public Response deleteMetaBcWlist(@Context HttpServletRequest req) {
		try {
			logger.debug("deleteMetaTableWlist");
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			Integer wordId = getNumberOrNull(req.getParameter("WORD_ID"));
			Integer bcId = getNumberOrNull(req.getParameter("BC_ID"));
			String column = req.getParameter("COLUMN").toString();
			SbiGlBnessClsWlistId id = new SbiGlBnessClsWlistId(wordId, bcId, column);

			dao.deleteMetaBnessClsWlist(id);

			JSONObject jo = new JSONObject();
			jo.put("Status", "OK");
			return Response.ok().build();

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@SuppressWarnings("unchecked")
	@POST
	@Path("/loadNavigationItem")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String loadNavigationItem(@Context HttpServletRequest req) {
		IGlossaryDAO dao = null;
		try {
			logger.debug("loadNavigationItem");
			dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			JSONObject requestVal = RestUtilities.readBodyAsJSONObject(req);
			JSONObject jo = new JSONObject();
			Map<String, Object> fin = dao.NavigationItem(requestVal);

			if (fin.containsKey("document")) {
				List<SbiObjects> doc = (List<SbiObjects>) fin.get("document");
				JSONArray ja = new JSONArray();
				for (SbiObjects tmp : doc) {
					// BIObject i= DAOFactory.getBIObjectDAO().toBIObject(tmp);
					ja.put(fromDocumentLight(tmp));
				}
				jo.put("document", ja);
				jo.put("document_size", fin.get("document_size"));
			}

			if (fin.containsKey("word")) {
				List<SbiGlWord> doc = (List<SbiGlWord>) fin.get("word");
				JSONArray ja = new JSONArray();
				for (SbiGlWord tmp : doc) {
					ja.put(fromWordLight(tmp));
				}
				jo.put("word", ja);
				jo.put("word_size", fin.get("word_size"));
			}

			if (fin.containsKey("dataset")) {
				List<SbiDataSet> doc = (List<SbiDataSet>) fin.get("dataset");
				JSONArray ja = new JSONArray();
				for (SbiDataSet tmp : doc) {
					ja.put(fromDataSetLight(tmp));
				}
				jo.put("dataset", ja);
				jo.put("dataset_size", fin.get("dataset_size"));
			}

			if (fin.containsKey("bness_cls")) {
				List<SbiMetaBc> doc = (List<SbiMetaBc>) fin.get("bness_cls");
				JSONArray ja = new JSONArray();
				for (SbiMetaBc tmp : doc) {
					ja.put(fromBnessClsLight(tmp));
				}
				jo.put("bness_cls", ja);
				jo.put("bness_cls_size", fin.get("bness_cls_size"));
			}

			if (fin.containsKey("table")) {
				List<SbiMetaTable> doc = (List<SbiMetaTable>) fin.get("table");
				JSONArray ja = new JSONArray();
				for (SbiMetaTable tmp : doc) {
					ja.put(fromTableLight(tmp));
				}
				jo.put("table", ja);
				jo.put("table_size", fin.get("table_size"));
			}

			jo.put("Status", "OK");
			return jo.toString();

		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/GlossaryUdpLikeLabel")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String loadUDPGlossaryLikeLabel(@Context HttpServletRequest req) {
		try {
			IUdpDAO dao = DAOFactory.getUdpDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			String lab = req.getParameter("LABEL");
			if (lab.trim().isEmpty()) {
				throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service. Empty search label");
			}
			List<Udp> lst = null;
			if (lab != null && !lab.trim().isEmpty()) {
				lst = dao.loadByFamilyAndLikeLabel("Glossary", lab);
			} else {
				lst = dao.loadAllByFamily("Glossary");
			}
			if (lst == null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service. Null list");
			}

			JSONArray jarr = new JSONArray();
			for (Udp o : lst) {
				jarr.put(fromUdpLight(o));
			}
			return jarr.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/listContents")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String listContents(@Context HttpServletRequest req) {
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			Integer glossaryId = getNumberOrNull(req.getParameter("GLOSSARY_ID"));
			Integer parentId = getNumberOrNull(req.getParameter("PARENT_ID"));

			List<SbiGlContents> lst = dao.listContentsByGlossaryIdAndParentId(glossaryId, parentId);

			JSONArray jarr = new JSONArray();
			if (lst != null) {
				for (Iterator<SbiGlContents> iterator = lst.iterator(); iterator.hasNext();) {
					SbiGlContents sbiGlContents = iterator.next();
					Integer wordCount = dao.CountWlistByContent(sbiGlContents.getContentId());
					Integer ContentCount = dao.CountContentChild(sbiGlContents.getContentId());
					jarr.put(fromContentsLight(sbiGlContents, (wordCount > 0), (ContentCount > 0)));
				}
			}

			if (parentId != null) {
				List<SbiGlWord> lstw = dao.listWlistWord(parentId);
				if (lstw != null) {
					for (SbiGlWord wl : lstw) {
						jarr.put(fromWordLight(wl));
					}
				}

			}

			return jarr.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/listGlossary")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String listGlossary(@Context HttpServletRequest req, @QueryParam("dateFilter") String dateFilter) {
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);

			List<SbiGlGlossary> lst = dao.listGlossary();
			JSONArray jarr = new JSONArray();
			if (lst != null) {
				for (Iterator<SbiGlGlossary> iterator = lst.iterator(); iterator.hasNext();) {
					SbiGlGlossary sbiGlGlossary = iterator.next();
					if (dateFilter != null) {
						if (sbiGlGlossary.getCommonInfo().getTimeUp() != null) {
							if (sbiGlGlossary.getCommonInfo().getTimeIn().getTime() >= new Date(dateFilter).getTime()
									|| sbiGlGlossary.getCommonInfo().getTimeUp().getTime() > new Date(dateFilter).getTime()) {
								jarr.put(fromGlossaryLight(sbiGlGlossary));
							}
						} else {
							if (sbiGlGlossary.getCommonInfo().getTimeIn().getTime() >= new Date(dateFilter).getTime()) {
								jarr.put(fromGlossaryLight(sbiGlGlossary));
							}
						}
					} else {
						jarr.put(fromGlossaryLight(sbiGlGlossary));

					}
				}
			}
			return jarr.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/listWords")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String listWords(@Context HttpServletRequest req) {
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			// TODO check if profile is null
			dao.setUserProfile(profile);
			String word = req.getParameter("WORD");
			Integer page = getNumberOrNull(req.getParameter("Page"));
			Integer itempp = getNumberOrNull(req.getParameter("ItemPerPage"));
			Integer gloss_id = getNumberOrNull(req.getParameter("GLOSSARY_ID"));

			List<SbiGlWord> lst = null;
			if ((word != null && !word.trim().isEmpty()) || gloss_id != null) {
				lst = dao.listWordFiltered(word, page, itempp, gloss_id);
			} else {
				lst = dao.listWord(page, itempp);
			}
			JSONArray jarr = new JSONArray();
			if (lst != null) {
				for (Iterator<SbiGlWord> iterator = lst.iterator(); iterator.hasNext();) {
					SbiGlWord sbiGlWord = iterator.next();
					jarr.put(fromWordLight(sbiGlWord));
				}
			}

			if (page != null && itempp != null) {
				JSONObject fin = new JSONObject();
				fin.put("item", jarr);
				fin.put("itemCount", dao.wordCount(word, gloss_id));
				return fin.toString();
			}

			return jarr.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getDocumentInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getDocumentInfo(@Context HttpServletRequest req) {

		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);

			Integer documentId = getNumberOrNull(req.getParameter("DOCUMENT_ID"));
			// String dtsws=req.getParameter("DATASETWORD");
			// Boolean datasWord=false;
			// if(dtsws!=null){
			// datasWord= dtsws.toString().compareTo("true")==0;
			// }
			if (documentId == null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "Input param Id [" + documentId + "] not valid or null");
			}

			JSONObject jobj = new JSONObject();

			List<SbiGlDocWlist> listDocWlist = dao.listDocWlist(documentId);

			JSONArray ja = new JSONArray();
			for (SbiGlDocWlist sb : listDocWlist) {
				ja.put(fromWordLight(sb.getWord()));
			}
			jobj.put("word", ja);

			return jobj.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getDataSetInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.GLOSSARY, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getDataSetInfo(@Context HttpServletRequest req) {

		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);

			Integer datasetId = getNumberOrNull(req.getParameter("DATASET_ID"));
			if (datasetId == null) {
				// checkif ther is a label of dataset
				boolean recovered = false;
				String dsl = req.getParameter("DATASET_LABEL");
				if (dsl != null) {
					SbiDataSet obj = DAOFactory.getSbiDataSetDAO().loadSbiDataSetByLabel(dsl);
					if (obj != null) {
						datasetId = obj.getId().getDsId();
						recovered = true;
					}
				}

				if (!recovered) {
					throw new SpagoBIServiceException(req.getPathInfo(), "Input param Id [" + datasetId + "] not valid or null");
				}
			}

			String organiz = req.getParameter("ORGANIZATION") == null ? null : req.getParameter("ORGANIZATION").toString();

			List<SbiGlDataSetWlist> listDatasetWlist = dao.listDataSetWlist(datasetId, organiz);
			JSONObject jo = new JSONObject();
			JSONArray ja = new JSONArray();
			JSONArray dsWord = new JSONArray();
			Map<String, Integer> map = new HashMap<String, Integer>();

			SbiDataSet datas = DAOFactory.getSbiDataSetDAO().loadSbiDataSetByIdAndOrganiz(datasetId, organiz);
			MetaData m = datas.getMetadata();
			if (m != null) {
				for (int i = 0; i < m.getFieldCount(); i++) {
					if (m.getFieldAlias(i).compareTo(DirectDSWordChild) != 0) {
						// if is not direct child of dataset

						map.put(m.getFieldAlias(i), ja.length());
						map.put(m.getFieldAlias(i), ja.length());
						JSONObject tmp = new JSONObject();
						tmp.put("alias", m.getFieldAlias(i));
						tmp.put("organization", datas.getId().getOrganization());
						tmp.put("datasetId", datas.getId().getDsId());
						tmp.put("word", new JSONArray());
						ja.put(tmp);
					}
				}

			}

			if (!listDatasetWlist.isEmpty()) {
				for (SbiGlDataSetWlist sb : listDatasetWlist) {
					if (sb.getId().getColumn_name().compareTo(DirectDSWordChild) != 0) {
						ja.getJSONObject(map.get(sb.getId().getColumn_name())).getJSONArray("word").put(fromWordLight(sb.getWord()));
					} else {
						dsWord.put(fromWordLight(sb.getWord()));
					}
				}
			}

			jo.put("SbiGlDataSetWlist", ja);
			jo.put("DataSet", JSON.parse(JsonConverter.objectToJson(datas, SbiDataSet.class)));
			jo.put("Word", dsWord);
			return jo.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getMetaTableInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getMetaTableInfo(@Context HttpServletRequest req) {

		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);

			Integer metaTableId = getNumberOrNull(req.getParameter("META_TABLE_ID"));
			if (metaTableId == null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "MetaTable id  [" + metaTableId + "] not valid or null");
			}

			JSONObject jo = new JSONObject();
			JSONArray columnArray = new JSONArray();
			JSONArray mtWord = new JSONArray();

			Map<String, Integer> map = new HashMap<String, Integer>();
			SbiMetaTable datas = DAOFactory.getSbiMetaTableDAO().loadTableWithColumnByID(metaTableId);
			for (Iterator<SbiMetaTableColumn> iterator = datas.getSbiMetaTableColumns().iterator(); iterator.hasNext();) {
				SbiMetaTableColumn smc = iterator.next();
				map.put(smc.getName(), columnArray.length());
				JSONObject joMC = new JSONObject();
				joMC.put("name", smc.getName());
				joMC.put("columnId", smc.getColumnId());
				joMC.put("type", smc.getType());
				joMC.put("word", new JSONArray());
				columnArray.put(joMC);
			}

			List<SbiGlTableWlist> listMetaTableWlist = dao.listMetaTableWlist(metaTableId);

			for (SbiGlTableWlist twl : listMetaTableWlist) {

				if (".SELF".equals(twl.getId().getColumn_name().toUpperCase())) {
					mtWord.put(fromWordLight(twl.getWord()));
				} else {
					columnArray.getJSONObject(map.get(twl.getId().getColumn_name())).getJSONArray("word").put(fromWordLight(twl.getWord()));
				}

			}

			jo.put("sbiGlTableWlist", columnArray);
			jo.put("metaTable", JSON.parse(JsonConverter.objectToJson(datas, SbiMetaTable.class)));
			jo.put("metaSource", JSON.parse(JsonConverter.objectToJson(datas.getSbiMetaSource(), SbiMetaSource.class)));
			jo.put("words", mtWord);
			return jo.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getMetaBcInfo")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getMetaBcInfo(@Context HttpServletRequest req) {

		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);

			Integer metaBcId = getNumberOrNull(req.getParameter("META_BC_ID"));
			if (metaBcId == null) {
				throw new SpagoBIServiceException(req.getPathInfo(), "MetaBc id  [" + metaBcId + "] not valid or null");
			}

			JSONObject jo = new JSONObject();
			JSONArray columnArray = new JSONArray();
			JSONArray mtWord = new JSONArray();

			Map<String, Integer> map = new HashMap<String, Integer>();
			SbiMetaBc datas = DAOFactory.getSbiMetaBCDAO().loadBcWithAttributesByID(metaBcId);
			for (Iterator<SbiMetaBcAttribute> iterator = datas.getSbiMetaBcAttributes().iterator(); iterator.hasNext();) {
				SbiMetaBcAttribute smc = iterator.next();
				map.put(smc.getName(), columnArray.length());
				JSONObject joMC = new JSONObject();
				joMC.put("name", smc.getName());
				joMC.put("columnId", smc.getAttributeId());
				joMC.put("type", smc.getType());
				joMC.put("word", new JSONArray());
				columnArray.put(joMC);
			}

			List<SbiGlBnessClsWlist> listMetaBcWlist = dao.listMetaBcWlist(metaBcId);
			// List<SbiGlTableWlist> listMetaTableWlist = dao.listMetaTableWlist(metaBcId);

			for (SbiGlBnessClsWlist twl : listMetaBcWlist) {

				if (".SELF".equals(twl.getId().getColumn_name().toUpperCase())) {
					mtWord.put(fromWordLight(twl.getWord()));
				} else {
					columnArray.getJSONObject(map.get(twl.getId().getColumn_name())).getJSONArray("word").put(fromWordLight(twl.getWord()));
				}

			}

			jo.put("sbiGlBnessClsWlist", columnArray);
			jo.put("metaBc", JSON.parse(JsonConverter.objectToJson(datas, SbiMetaBc.class)));
			jo.put("words", mtWord);
			return jo.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getWord")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.GLOSSARY, SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getWord(@Context HttpServletRequest req) {
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			String id = req.getParameter("WORD_ID");
			String name = req.getParameter("WORD_NAME");
			if (id == null || !id.matches("\\d+")) {
				boolean resolved = false;
				// check if label is present
				if (name != null) {
					List<SbiGlWord> lw = dao.loadWordByName(name);
					if (lw.size() != 0) {
						id = "" + lw.get(0).getWordId();
						resolved = true;
					}
				}

				if (!resolved) {
					JSONObject jo = new JSONObject();
					jo.put("Status", "NON OK");
					jo.put("Message", "sbi.generic.not.found");
					return jo.toString();
				}
			}

			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);
			SbiGlWord word = dao.loadWord(Integer.valueOf(id));

			JSONObject jobj = fromWord(word);
			return jobj.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getGlossary")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getGlossary(@Context HttpServletRequest req) {
		String glossaryId = req.getParameter("GLOSSARY_ID");
		if (!glossaryId.matches("\\d+")) {
			throw new SpagoBIServiceException(req.getPathInfo(), "Input param Id [" + glossaryId + "] not valid");
		}
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);
			// Collection roles = profile.getRoles();
			// String role = (String)((ArrayList)roles).get(0);
			// Boolean role2 = (Boolean)((ArrayList)roles).contains("/spagobi/admin");
			// profile.
			// profile..isAbleToExecuteAction(SpagoBIConstants.DOCUMENT_STATE_MANAGEMENT);

			SbiGlGlossary glo = dao.loadGlossary(Integer.valueOf(glossaryId));
			JSONObject jobj = fromGlossary(glo);
			return jobj.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/getContent")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String getContents(@Context HttpServletRequest req) {
		String contentId = req.getParameter("CONTENT_ID");
		if (!contentId.matches("\\d+")) {
			throw new SpagoBIServiceException(req.getPathInfo(), "Input param Id [" + contentId + "] not valid");
		}
		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);

			SbiGlContents cont = dao.loadContents(Integer.valueOf(contentId));
			JSONObject jobj = fromContent(cont);
			return jobj.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

	@GET
	@Path("/glosstreeLike")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.MANAGE_GLOSSARY_BUSINESS, SpagoBIConstants.MANAGE_GLOSSARY_TECHNICAL })
	public String glosstreeLike(@Context HttpServletRequest req) {

		try {
			IGlossaryDAO dao = DAOFactory.getGlossaryDAO();
			IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);
			dao.setUserProfile(profile);
			JSONObject jo = new JSONObject();

			String word = req.getParameter("WORD");
			String glossaryId = req.getParameter("GLOSSARY_ID");
			JSONObject fin = new JSONObject();

			if (word != null && word.trim().isEmpty()) {
				SbiGlGlossary gl = dao.loadGlossary(Integer.parseInt(glossaryId));
				fin = fromGlossaryLight(gl);
				fin.put("SBI_GL_CONTENTS", JSON.parse(this.listContents(req)));
			} else {
				fin = dao.glosstreeLike(glossaryId, word);
			}

			jo.put("Status", "OK");
			jo.put("GlossSearch", fin);
			return jo.toString();
		} catch (Throwable t) {
			throw new SpagoBIServiceException(req.getPathInfo(), "An unexpected error occured while executing service", t);
		}
	}

}