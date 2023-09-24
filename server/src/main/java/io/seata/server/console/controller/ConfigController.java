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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/admin/configuration")
public class ConfigController {

    private final Object SET_CONF_LOCK = new Object();

    @Autowired
    private ConfigurationService configurationService;

    @RequestMapping(method = RequestMethod.GET)
    SingleResult<Map<String, String>> getConf(String key) {
        Configuration instance = ConfigurationFactory.getInstance();
        Map<String, String> configurations = new HashMap<>();
        if (key == null) {
            Set<String> availableConf = ConfigValidator.availableConfiguration();
            for (String confKey: availableConf) {
                configurations.put(confKey, instance.getConfig(confKey));
            }
        } else {
            configurations.put(key, instance.getConfig(key));
        }
        return SingleResult.success(configurations);
    }

    @RequestMapping(method = RequestMethod.POST)
    SingleResult<Map<String, String>> setConf(@RequestBody Map<String, String> properties) {
        Map<String, String> result = new HashMap<>(properties.size());

        synchronized (SET_CONF_LOCK) {
            for (String key: properties.keySet()) {
                ConfigValidator.ValidateResult validateResult = ConfigValidator.validateConfiguration(key, properties.get(key));
                if (!validateResult.getValid())
                    return SingleResult.failure(Code.ERROR.code, validateResult.getErrorMessage());
                if (!ConfigValidator.canBeConfiguredDynamically(key))
                    return SingleResult.failure(Code.ERROR.code, "Cannot be configured dynamically");
                boolean success = ConfigurationFactory.getInstance().putConfig(key, properties.get(key));
                if (!success) {
                    return SingleResult.failure(Code.ERROR.code, "Set config failed");
                }
            }

            configurationService.reloadConfiguration();

            for (String key: properties.keySet()) {
                result.put(key, ConfigurationFactory.getInstance().getConfig(key));
            }
        }

        return SingleResult.success(result);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/validate")
    SingleResult<Map<String, Boolean>> ValidateConf(@RequestBody Map<String, String> properties) {
        Map<String, Boolean> result = new HashMap<>(properties.size());
        for (String key: properties.keySet()) {
            ConfigValidator.ValidateResult validateResult = ConfigValidator.validateConfiguration(key, properties.get(key));
            result.put(key, validateResult.getValid());
        }
        return SingleResult.success(result);
    }

    @RequestMapping(value = "/reload", method = RequestMethod.POST)
    SingleResult<String> reloadConf() {
        configurationService.reloadConfiguration();
        return SingleResult.success("");
    }
}
