package edu.txstate.tracs.warehouse;

import edu.txstate.tracs.jdbc.BaseJdbcDao;

import java.util.List;
import java.util.Map;

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

    public Map<String, Boolean> getUserConfidentialMap(List<String> netids) {
        Object userConfidentialMap = queryForObject("get_confidential_map_from_person", netids, Boolean.class);
        if (userConfidentialMap == null) {
            return null;
        }
        return (Map<String, Boolean>)userConfidentialMap;
    }

}
