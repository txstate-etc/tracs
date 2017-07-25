package edu.txstate.tracs.warehouse;

import java.util.List;
import java.util.Map;

public class WarehouseDatabaseService implements WarehouseService {

    private WarehouseDatabaseDao warehouseDao;
    public void setWarehouseDao(WarehouseDatabaseDao dao) { warehouseDao = dao; }

    public boolean isUserConfidential(String netid) {
        return warehouseDao.isUserConfidential(netid);
    }

    public Map<String, Boolean> getUserConfidentailMap(List<String> netids) {
        return warehouseDao.getUserConfidentialMap(netids);
    }
}
