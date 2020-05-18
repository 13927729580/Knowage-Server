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
package it.eng.spagobi.tools.scheduler.jobs;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import it.eng.spagobi.tools.dataset.resource.export.ResourceExportFolderCleaningManager;

public class ResourceExportFolderCleaningJob extends AbstractSpagoBIJob implements Job {

	static private Logger logger = Logger.getLogger(ResourceExportFolderCleaningJob.class);

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		logger.debug("IN");
		try {
			this.setTenant(jobExecutionContext);
			executeInternal();
		} finally {
			this.unsetTenant();
			logger.debug("OUT");
		}
	}

	private void executeInternal() {

		logger.debug("IN");
		try {
			ResourceExportFolderCleaningManager resourceExportFolderCleaningManager = new ResourceExportFolderCleaningManager();
			resourceExportFolderCleaningManager.executeCleaning();
			logger.debug("Resource export folder cleaning ended succesfully!");
		} catch (Exception e) {
			logger.error("Error while executiong job ", e);
		} finally {
			logger.debug("OUT");
		}
	}
}
