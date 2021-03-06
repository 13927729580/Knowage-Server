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
package it.eng.spagobi.tools.distributionlist.service;

import it.eng.spago.base.RequestContainer;
import it.eng.spago.base.SessionContainer;
import it.eng.spago.base.SourceBean;
import it.eng.spago.dispatching.module.list.basic.AbstractBasicListModule;
import it.eng.spago.error.EMFErrorSeverity;
import it.eng.spago.error.EMFUserError;
import it.eng.spago.paginator.basic.ListIFace;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.services.DelegatedHibernateConnectionListService;
import it.eng.spagobi.commons.utilities.AuditLogUtilities;
import it.eng.spagobi.commons.utilities.HibernateSessionManager;
import it.eng.spagobi.commons.utilities.UserUtilities;
import it.eng.spagobi.services.common.SsoServiceInterface;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.HibernateException;
import org.hibernate.Session;

/**
 * Loads the Distributionlist List
 */
public class ListDistributionListUserModule extends AbstractBasicListModule{

	public static final String MODULE_PAGE = "ListDistributionListUserPage";

	/**
	 * Gets the list.
	 * 
	 * @param request The request SourceBean
	 * @param response The response SourceBean
	 * 
	 * @return ListIFace
	 * 
	 * @throws Exception the exception
	 */

	public ListIFace getList(SourceBean request, SourceBean response) throws Exception {

		//Start writing log in the DB
		Session aSession =null;
		try {
			aSession = HibernateSessionManager.getCurrentSession();
			//Connection jdbcConnection = aSession.connection();
			Connection jdbcConnection = HibernateSessionManager.getConnection(aSession);
			IEngUserProfile profile = UserUtilities.getUserProfile();
			AuditLogUtilities.updateAudit(((HttpServletRequest)getRequestContainer().getRequestContainer().getInternalRequest()),  profile, "DISTRIBUTION_LIST.OPEN", null, "OK");
		} catch (HibernateException he) {
			throw new EMFUserError(EMFErrorSeverity.ERROR, 100);
		} finally {
			if (aSession!=null){
				if (aSession.isOpen()) aSession.close();
			}
		}
		//End writing log in the DB

		RequestContainer aRequestContainer = RequestContainer.getRequestContainer();
		SessionContainer aSessionContainer = aRequestContainer.getSessionContainer();

		IEngUserProfile userProfile =UserUtilities.getUserProfile();
		String userId="";
		if (userProfile!=null) userId=(String)((UserProfile)userProfile).getUserId();
		//sets the userid as input parameter for the query fo statements.xml
		aSessionContainer.setAttribute(SsoServiceInterface.USER_ID ,userId);


		Collection c = null;

		c = ((UserProfile)userProfile).getRolesForUse();


		Iterator i = c.iterator();
		int j = 0;
		while (i.hasNext()){
			String roles = (String)i.next();
			aSessionContainer.setAttribute("role"+j,roles);
			j++ ;
		}
		while (j<6){
			String s= "/";
			aSessionContainer.setAttribute("role"+j,s);
			j++ ;
		}

		return DelegatedHibernateConnectionListService.getList(this, request, response);
	} 


}
