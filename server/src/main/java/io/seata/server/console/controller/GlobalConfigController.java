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
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.constant.Code;
import io.seata.console.result.SingleResult;
import io.seata.server.console.param.ConfigurationParam;
import io.seata.server.console.service.GlobalConfigService;
import io.seata.server.console.vo.GlobalConfigVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;


/**
 * Global Config Controller
 * @author: Yuzhiqiang
 */
@RestController
@RequestMapping(value = "/api/v1/console/editconfig")
public class GlobalConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfigController.class);

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Resource(type = GlobalConfigService.class)
    private GlobalConfigService globalConfigDBService;

    @RequestMapping(value = "/putConfig", method = RequestMethod.POST)
    public SingleResult<Boolean> putConfig(@RequestBody ConfigurationParam param)  {

        try {
            boolean result = CONFIG.putConfig(param.getDataId(), param.getContent());
            if (result) {
                LOGGER.info("Put config '{} = {}' succeed.", param.getDataId(), param.getContent());
                return SingleResult.success(result);
            } else {
                LOGGER.error("Put config '{} = {}' failed.", param.getDataId(), param.getContent());
                return SingleResult.failure(Code.ERROR);
            }
        } catch (Exception e) {
            LOGGER.error("Put config '{} = {}' failed.", param.getDataId(), param.getContent(), e);
            return SingleResult.failure("101", "config exception");
        }
    }

    @RequestMapping(value = "/getConfigList", method = RequestMethod.GET)
    public SingleResult<List<GlobalConfigVO>> getConfigList()  {
        return SingleResult.success(globalConfigDBService.getConfigList());
    }
}
