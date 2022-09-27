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
import io.seata.server.console.service.GlobalConfigService;
import io.seata.server.console.vo.GlobalConfigVO;
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

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Resource(type = GlobalConfigService.class)
    private GlobalConfigService globalConfigDBService;

    @RequestMapping(value = "/putconfig", method = RequestMethod.POST)
    public SingleResult<Boolean> putconfig(String dataId, String content)  {

        try {
            boolean result = CONFIG.putConfig(dataId, content);
            if (result) {
                return SingleResult.success(result);
            } else {
                return SingleResult.failure(Code.ERROR);
            }
        } catch (Exception e) {
            return SingleResult.failure("101", "config exception");
        }
    }

    @RequestMapping(value = "/getconfiglist", method = RequestMethod.GET)
    public List<GlobalConfigVO> get()  {
        return globalConfigDBService.getConfigList();
    }
}
