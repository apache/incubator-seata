/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.seata.server.console.controller;

import io.seata.config.ConfigValidator;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.server.console.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/admin/registry")
public class RegistryController {
    private Logger LOGGER = LoggerFactory.getLogger(RegistryController.class);

    @Autowired
    private ConfigurationService configurationService;

    private final Object SET_CONF_LOCK = new Object();

    @RequestMapping(method = RequestMethod.GET)
    SingleResult<Map<String, String>> getRegistryConf(String key) {
        Configuration instance = ConfigurationFactory.getInstance();
        Map<String, String> configurations = new HashMap<>();
        if (key == null) {
            Set<String> availableProperties = ConfigValidator.availableRegistryConf();
            for (String item: availableProperties) {
                configurations.put(item, instance.getConfig(item));
            }
        } else {
            if (ConfigValidator.availableRegistryConf().contains(key)) {
                configurations.put(key, instance.getConfig(key));
            }
        }
        return SingleResult.success(configurations);
    }

    @RequestMapping(method = RequestMethod.POST)
    SingleResult<Map<String, String>> setRegistryConf(@RequestBody Map<String, String> properties) {
        Map<String, String> result = new HashMap<>(properties.size());

        synchronized (SET_CONF_LOCK) {
            for (String key : properties.keySet()) {
                ConfigValidator.ValidateResult validateResult = ConfigValidator.validateRegistryConf(key, properties.get(key));
                if (!validateResult.getValid())
                    return SingleResult.failure(Code.ERROR.code, validateResult.getErrorMessage());
                if (!ConfigValidator.canBeConfiguredDynamically(key))
                    return SingleResult.failure(Code.ERROR.code, "Cannot be configured dynamically");
                System.setProperty(key, properties.get(key));
                result.put(key, properties.get(key));
            }

            boolean success = configurationService.reloadRegistryConfiguration();
            if (!success) return SingleResult.failure(Code.ERROR);

            result.replaceAll((k, v) -> ConfigurationFactory.getInstance().getConfig(k));
        }

        return SingleResult.success(result);
    }
}
