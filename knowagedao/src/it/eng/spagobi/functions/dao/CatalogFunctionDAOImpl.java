package it.eng.spagobi.functions.dao;

import it.eng.spago.error.EMFUserError;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.commons.bo.Domain;
import it.eng.spagobi.commons.dao.AbstractHibernateDAO;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.dao.SpagoBIDOAException;
import it.eng.spagobi.functions.metadata.SbiCatalogFunction;
import it.eng.spagobi.functions.metadata.SbiFunctionInputDataset;
import it.eng.spagobi.functions.metadata.SbiFunctionInputDatasetId;
import it.eng.spagobi.functions.metadata.SbiFunctionInputVariable;
import it.eng.spagobi.functions.metadata.SbiFunctionInputVariableId;
import it.eng.spagobi.functions.metadata.SbiFunctionOutput;
import it.eng.spagobi.functions.metadata.SbiFunctionOutputId;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.dao.IDataSetDAO;
import it.eng.spagobi.utilities.CatalogFunction;
import it.eng.spagobi.utilities.assertion.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CatalogFunctionDAOImpl extends AbstractHibernateDAO implements ICatalogFunctionDAO {

	static private Logger logger = Logger.getLogger(CatalogFunctionDAOImpl.class);

	@Override
	public List<SbiCatalogFunction> loadAllCatalogFunctions() {
		logger.debug("IN");
		Session session = null;
		List<SbiCatalogFunction> sbiCatalogFunctions = null;
		try {
			session = getSession();
			Assert.assertNotNull(session, "session cannot be null");
			Query hibQuery = session.createQuery("from SbiCatalogFunction");
			sbiCatalogFunctions = hibQuery.list();
		} catch (Throwable t) {
			throw new SpagoBIDOAException("An error occured while reading Catalog Functions from DB", t);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}
		return sbiCatalogFunctions;
	}

	@Override
	public int insertCatalogFunction(CatalogFunction catalogFunction, List<String> inputDatasets, Map<String, String> inputVariables,
			Map<String, String> outputs) {

		int catalogFunctionId = -1;
		Session session;
		Transaction transaction;

		logger.debug("IN");

		session = null;
		transaction = null;
		try {
			try {
				session = getSession();
				Assert.assertNotNull(session, "session cannot be null");
				transaction = session.beginTransaction();
				Assert.assertNotNull(transaction, "transaction cannot be null");
			} catch (Throwable t) {
				throw new SpagoBIDOAException("An error occured while creating the new transaction", t);
			}
			SbiCatalogFunction hibMap = toSbiFunctionCatalog(catalogFunction);
			updateSbiCommonInfo4Insert(hibMap);
			catalogFunctionId = (int) session.save(hibMap);

			SbiCatalogFunction hibCatFunction = (SbiCatalogFunction) session.load(SbiCatalogFunction.class, catalogFunctionId);

			hibCatFunction.setSbiFunctionInputVariables(getSbiFunctionInputVariablesSet(inputVariables, hibCatFunction));
			hibCatFunction.setSbiFunctionOutputs(getSbiFunctionOutputSet(outputs, hibCatFunction));
			hibCatFunction.setSbiFunctionInputDatasets(getSbiFunctionInputDatasetSet(inputDatasets, hibCatFunction));
			session.saveOrUpdate(hibCatFunction);

			transaction.commit();

		} catch (Throwable t) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}

			throw new SpagoBIDOAException("An unexpected error occured while inserting catalog function", t);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}

		return catalogFunctionId;
	}

	private Set<SbiFunctionInputDataset> getSbiFunctionInputDatasetSet(List<String> inputDatasets, SbiCatalogFunction sbiCatalogFunction) {

		Set<SbiFunctionInputDataset> inputDatasetSet = new HashSet<SbiFunctionInputDataset>();
		SbiFunctionInputDataset dataset = null;
		IEngUserProfile profile = getUserProfile();

		for (String datasetLabel : inputDatasets) {

			// Create and Insert Dataset

			IDataSetDAO dataSetDAO = null;
			try {
				dataSetDAO = DAOFactory.getDataSetDAO();
			} catch (EMFUserError e) {
				throw new SpagoBIDOAException("An error occured while getting the dataset DAO", e);
			}
			dataSetDAO.setUserProfile(profile);

			logger.debug("check if dataset with label " + datasetLabel + " is already present");

			// check label is already present; insert or modify dependently
			IDataSet iDataSet = dataSetDAO.loadDataSetByLabel(datasetLabel);
			int dsId = iDataSet.getId();
			dataset = new SbiFunctionInputDataset(new SbiFunctionInputDatasetId(sbiCatalogFunction.getFunctionId(), dsId));
			inputDatasetSet.add(dataset);
		}

		return inputDatasetSet;

	}

	private Set<SbiFunctionInputVariable> getSbiFunctionInputVariablesSet(Map<String, String> inputVariables, SbiCatalogFunction sbiCatalogFunction) {

		Set<SbiFunctionInputVariable> inputVarSet = new HashSet<SbiFunctionInputVariable>();
		SbiFunctionInputVariable var = null;

		for (String varName : inputVariables.keySet()) {
			String value = inputVariables.get(varName);
			var = new SbiFunctionInputVariable(new SbiFunctionInputVariableId(sbiCatalogFunction.getFunctionId(), varName), sbiCatalogFunction, value);
			inputVarSet.add(var);
		}

		return inputVarSet;
	}

	private Set<SbiFunctionOutput> getSbiFunctionOutputSet(Map<String, String> outputs, SbiCatalogFunction sbiCatalogFunction) {

		Set<SbiFunctionOutput> outputVariablesSet = new HashSet<SbiFunctionOutput>();
		SbiFunctionOutput var = null;
		Domain domain = null;

		for (String varLabel : outputs.keySet()) {
			String outType = outputs.get(varLabel);
			try {
				domain = DAOFactory.getDomainDAO().loadDomainByCodeAndValue("FUNCTION_OUTPUT", outType);
				Integer outTypeSbiDomainId = domain.getValueId();
				var = new SbiFunctionOutput(new SbiFunctionOutputId(sbiCatalogFunction.getFunctionId(), varLabel), sbiCatalogFunction, outTypeSbiDomainId);
				outputVariablesSet.add(var);
			} catch (EMFUserError e) {
				throw new SpagoBIDOAException("An error occured while getting domain by code [FUNCTION_OUTPUT] and value [" + outType + "]", e);
			}

		}

		return outputVariablesSet;
	}

	private SbiCatalogFunction toSbiFunctionCatalog(CatalogFunction functionItem) {

		SbiCatalogFunction hibFunctionCatalogItem = new SbiCatalogFunction();
		hibFunctionCatalogItem.setFunctionId(functionItem.getFunctionId());
		hibFunctionCatalogItem.setLanguage(functionItem.getLanguage());
		hibFunctionCatalogItem.setName(functionItem.getName());
		hibFunctionCatalogItem.setDescription(functionItem.getDescription());
		hibFunctionCatalogItem.setScript(functionItem.getScript());
		hibFunctionCatalogItem.setOwner(functionItem.getOwner());
		hibFunctionCatalogItem.setLabel(functionItem.getLabel());
		hibFunctionCatalogItem.setType(functionItem.getType());

		List<String> keywords = functionItem.getKeywords();
		String keywordsString = StringUtils.join(keywords.iterator(), ",");
		hibFunctionCatalogItem.setKeywords(keywordsString);

		return hibFunctionCatalogItem;
	}

	@Override
	public int updateCatalogFunction(CatalogFunction updatedCatalogFunction, int catalogFunctionId) {

		Session session;
		Transaction transaction;

		logger.debug("IN");

		session = null;
		transaction = null;
		try {
			try {
				session = getSession();
				Assert.assertNotNull(session, "session cannot be null");
				transaction = session.beginTransaction();
				Assert.assertNotNull(transaction, "transaction cannot be null");
			} catch (Throwable t) {
				throw new SpagoBIDOAException("An error occured while creating the new transaction", t);
			}

			SbiCatalogFunction hibCatFunction = (SbiCatalogFunction) session.get(SbiCatalogFunction.class, catalogFunctionId);
			hibCatFunction.getSbiFunctionInputDatasets().clear();
			for (Object o : hibCatFunction.getSbiFunctionInputDatasets()) {
				SbiFunctionInputDataset di = (SbiFunctionInputDataset) o;
				session.delete(di);
			}
			for (Object o : hibCatFunction.getSbiFunctionOutputs()) {
				SbiFunctionOutput out = (SbiFunctionOutput) o;
				session.delete(out);
			}
			hibCatFunction.setSbiFunctionOutputs(null);

			for (Object o : hibCatFunction.getSbiFunctionInputVariables()) {
				SbiFunctionInputVariable vi = (SbiFunctionInputVariable) o;
				session.delete(vi);
			}
			if (hibCatFunction.getSbiFunctionInputVariables() != null)
				hibCatFunction.getSbiFunctionInputVariables().clear();
			if (hibCatFunction.getSbiFunctionOutputs() != null)
				hibCatFunction.getSbiFunctionOutputs().clear();
			if (hibCatFunction.getSbiFunctionInputDatasets() != null)
				hibCatFunction.getSbiFunctionInputDatasets().clear();

			transaction.commit();
			session.close();

			try {
				session = getSession();
				Assert.assertNotNull(session, "session cannot be null");
				transaction = session.beginTransaction();
				Assert.assertNotNull(transaction, "transaction cannot be null");
			} catch (Throwable t) {
				throw new SpagoBIDOAException("An error occured while creating the new transaction", t);
			}
			hibCatFunction.setSbiFunctionInputVariables(getSbiFunctionInputVariablesSet(updatedCatalogFunction.getInputVariables(), hibCatFunction));
			hibCatFunction.setSbiFunctionOutputs(getSbiFunctionOutputSet(updatedCatalogFunction.getOutputs(), hibCatFunction));
			hibCatFunction.setSbiFunctionInputDatasets(getSbiFunctionInputDatasetSet(updatedCatalogFunction.getInputDatasets(), hibCatFunction));
			hibCatFunction.setLanguage(updatedCatalogFunction.getLanguage());
			hibCatFunction.setName(updatedCatalogFunction.getName());
			hibCatFunction.setDescription(updatedCatalogFunction.getDescription());
			hibCatFunction.setScript(updatedCatalogFunction.getScript());
			hibCatFunction.setOwner(updatedCatalogFunction.getOwner());
			hibCatFunction.setKeywords(StringUtils.join(updatedCatalogFunction.getKeywords().iterator(), ","));
			hibCatFunction.setLabel(updatedCatalogFunction.getLabel());
			hibCatFunction.setType(updatedCatalogFunction.getType());

			updateSbiCommonInfo4Update(hibCatFunction);
			session.saveOrUpdate(hibCatFunction);
			transaction.commit();

		} catch (Throwable t) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}

			throw new SpagoBIDOAException("An unexpected error occured while updating catalog function", t);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}

		return catalogFunctionId;

	}

	@Override
	public void deleteCatalogFunction(int idFunctionToDelete) {

		Session session;
		Transaction transaction;

		logger.debug("IN");

		session = null;
		transaction = null;
		try {
			session = getSession();
			Assert.assertNotNull(session, "session cannot be null");
			transaction = session.beginTransaction();
			Assert.assertNotNull(transaction, "transaction cannot be null");

			SbiCatalogFunction hibCatFunction = (SbiCatalogFunction) session.get(SbiCatalogFunction.class, idFunctionToDelete);
			session.delete(hibCatFunction);

			transaction.commit();

		} catch (Throwable t) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}

			throw new SpagoBIDOAException("An unexpected error occured while updating catalog function", t);
		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}

	}

	@Override
	public SbiCatalogFunction getCatalogFunctionById(int functionId) {

		Session session;
		Transaction transaction = null;
		SbiCatalogFunction sbiCatalogFunction = null;

		logger.debug("IN");

		session = null;
		try {

			session = getSession();
			Assert.assertNotNull(session, "session cannot be null");
			transaction = session.beginTransaction();
			sbiCatalogFunction = (SbiCatalogFunction) session.get(SbiCatalogFunction.class, functionId);
			transaction.commit();

		} catch (Throwable t) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			throw new SpagoBIDOAException("An error occured while reading Catalog Functions from DB", t);

		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}
		return sbiCatalogFunction;

	}

	@Override
	public SbiCatalogFunction getCatalogFunctionByLabel(String organization, String functionLabel) {

		Session session;
		Transaction transaction = null;
		SbiCatalogFunction sbiCatalogFunction = null;

		logger.debug("IN");

		session = null;
		try {

			session = getSession();
			Assert.assertNotNull(session, "session cannot be null");
			transaction = session.beginTransaction();
			// sbiCatalogFunction = (SbiCatalogFunction) session.get(SbiCatalogFunction.class, functionId);
			// TODO mettere una hibernate Query

			String hql = "FROM SbiCatalogFunction F WHERE F.commonInfo.organization = ? and F.label = ?";
			Query query = session.createQuery(hql);
			query.setString(0, organization);
			query.setString(1, functionLabel);
			List<SbiCatalogFunction> results = query.list();
			sbiCatalogFunction = results.get(0);

			transaction.commit();

		} catch (Throwable t) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			throw new SpagoBIDOAException("An error occured while reading Catalog Functions from DB", t);

		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
			logger.debug("OUT");
		}
		return sbiCatalogFunction;

	}
}
