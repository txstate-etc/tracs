package edu.txstate.tracs.warehouse;

import java.util.Map;
import java.util.Set;

public interface WarehouseService {
    public boolean isUserConfidential(String netid);
    public Map<String, Boolean> getUsersConfidentialMap(Set<String> netids);
}
