package org.apache.seata.mcp.service;

import java.util.Map;

public interface ModifyConfirmService {

    Map<String,String> confirmAndGetKey();

    Boolean isValidKey(String key);
}
