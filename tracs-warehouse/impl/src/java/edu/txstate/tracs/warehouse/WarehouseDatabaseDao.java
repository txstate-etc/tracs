package edu.txstate.tracs.warehouse;

import edu.txstate.tracs.jdbc.BaseJdbcDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class WarehouseDatabaseDao extends BaseJdbcDao {
    private static final Logger logger = LoggerFactory.getLogger(WarehouseDatabaseDao.class);

    public boolean isUserConfidential(String netid) {
        Object confidential = queryForObject("get_confidential_from_person", netid, Boolean.class);
        if (confidential == null) {
            return false;
        }
        return (Boolean)confidential;
    }

    public Map<String, Boolean> getUsersConfidentialMap(Set<String> netids) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("netids", netids);
        List<Map<String, Object>>  result = queryForList("get_confidential_map_from_person", parameters);
        Map<String, Boolean> usersConfidentialMap = new HashMap<String,Boolean>();
        for (Map m: result) {
            usersConfidentialMap.put(m.get("netid").toString(), (Boolean) m.get("confidential"));
        }

        if (usersConfidentialMap == null) {
            return null;
        }
        return usersConfidentialMap;
    }

}
