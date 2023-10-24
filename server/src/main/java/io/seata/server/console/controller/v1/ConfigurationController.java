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

package io.seata.server.console.controller.v1;

import io.seata.config.ConfigValidator;
import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.server.console.service.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Junduo Dong
 */
@RestController
@RequestMapping("/api/v1/console/configuration")
public class ConfigurationController {

    private final Object setConfLock = new Object();

    @Autowired
    private ConfigurationService configurationService;

    @RequestMapping(method = RequestMethod.POST, value = "/get")
    SingleResult<Map<String, String>> getConf(@RequestBody List<String> keys) {
        return SingleResult.success(configurationService.getConf(keys));
    }

    @RequestMapping(method = RequestMethod.POST)
    SingleResult<Map<String, List<String>>> setConf(@RequestBody Map<String, String> conf) {
        // TODO: replace that process-level lock with global lock
        synchronized (setConfLock) {
            try {
                return SingleResult.success(configurationService.setConf(conf));
            } catch (Exception e) {
                return SingleResult.failure(Code.ERROR.code, e.getMessage());
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/registry")
    SingleResult<Map<String, List<String>>> setRegistryConf(@RequestBody Map<String, String> conf) {
        // TODO: replace that process-level lock with global lock
        synchronized (setConfLock) {
            try {
                return SingleResult.success(configurationService.setRegistryConf(conf));
            } catch (Exception e) {
                return SingleResult.failure(Code.ERROR.code, e.getMessage());
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/config-center")
    SingleResult<Map<String, List<String>>> setConfigCenterConf(@RequestBody Map<String, String> conf) {
        // TODO: replace that process-level lock with global lock
        synchronized (setConfLock) {
            try {
                return SingleResult.success(configurationService.setConfCenterConf(conf));
            } catch (Exception e) {
                return SingleResult.failure(Code.ERROR.code, e.getMessage());
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/validate")
    SingleResult<Map<String, Boolean>> validateConf(@RequestBody Map<String, String> conf) {
        Map<String, Boolean> result = new HashMap<>(conf.size());
        for (String key: conf.keySet()) {
            ConfigValidator.ValidateResult validateResult = ConfigValidator.validateConfiguration(key, conf.get(key));
            result.put(key, validateResult.getValid());
        }
        return SingleResult.success(result);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/reload")
    SingleResult<Boolean> reloadConf() {
        // TODO: replace that process-level lock with global lock
        synchronized (setConfLock) {
            try {
                configurationService.reloadConfigurationInstance();
                return SingleResult.success(true);
            } catch (Exception e) {
                return SingleResult.success(false);
            }
        }
    }
}
