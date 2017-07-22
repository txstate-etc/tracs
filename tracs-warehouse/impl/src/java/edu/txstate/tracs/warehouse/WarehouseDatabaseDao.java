package edu.txstate.tracs.warehouse;

import edu.txstate.tracs.jdbc.BaseJdbcDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WarehouseDatabaseDao extends BaseJdbcDao {
    private static final Log LOG = LogFactory.getLog(WarehouseDatabaseDao.class);

    public boolean isUserConfidential(String netid) {
        Object confidential = queryForObject("get_confidential_from_person", netid, Boolean.class);
        if (confidential == null) {
            return false;
        }
        return (Boolean)confidential;
    }
}
