package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ModifyConfirmTools {

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyConfirmTools.class);

    @Tool(
            description =
                    "Before modifying a transaction or lock, the user calls this function to obtain the operation key")
    public Map<String, String> confirmAndGetKey() {
        Map<String, String> keyMap = modifyConfirmService.confirmAndGetKey();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("the user obtains a modify key:{}", keyMap.get("modify_key"));
        }
        return keyMap;
    }
}
