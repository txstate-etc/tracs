package edu.txstate.tracs.warehouse;

public class WarehouseDatabaseService implements WarehouseService {

    private WarehouseDatabaseDao warehouseDao;
    public void setWarehouseDao(WarehouseDatabaseDao dao) { warehouseDao = dao; }

    public boolean isUserConfidential(String netid) {
        return warehouseDao.isUserConfidential(netid);
    }
}
