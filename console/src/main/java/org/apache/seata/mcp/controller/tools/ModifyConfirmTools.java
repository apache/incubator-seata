/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                    "Before modifying(update or delete) a transaction or lock, the user calls this function to obtain the operation key")
    public Map<String, String> confirmAndGetKey() {
        Map<String, String> keyMap = modifyConfirmService.confirmAndGetKey();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("the user obtains a modify key:{}", keyMap.get("modify_key"));
        }
        return keyMap;
    }
}
