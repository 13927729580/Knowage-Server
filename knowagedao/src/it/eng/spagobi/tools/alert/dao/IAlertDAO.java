package it.eng.spagobi.tools.alert.dao;

import it.eng.spagobi.commons.dao.ISpagoBIDao;
import it.eng.spagobi.tools.alert.bo.Alert;
import it.eng.spagobi.tools.alert.bo.AlertAction;
import it.eng.spagobi.tools.alert.bo.AlertListener;

import java.util.List;

public interface IAlertDAO extends ISpagoBIDao {

	public List<AlertListener> listListener();

	public List<AlertAction> listAction();

	public AlertListener loadListener(Integer id);

	public AlertAction loadAction(Integer id);

	public Integer insert(Alert alert);

	public void update(Alert alert);

	public List<Alert> listAlert();

	public Alert loadAlert(Integer id);

	public void remove(Integer id);

}
