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
package io.seata.server.console.impl.db;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.PageResult;
import io.seata.server.console.service.GlobalConfigService;
import io.seata.server.console.vo.GlobalConfigVO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Global Config  ServiceImpl
 * @author: Yuzhiqiang
 */
@Service
public class GlobalConfigDBServiceImpl implements GlobalConfigService {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public PageResult<GlobalConfigVO> getConfigList() {

        List<GlobalConfigVO> list = new ArrayList<>();
        String[] configKey = {"1", "2", "3", "4", "5", "6"};
        String[] descr = {"11111", "22222", "33333", "44444", "55555", "66666"};
        for (int i = 0; i < configKey.length; i++) {
            String config = configKey[i];
            String value = CONFIG.getConfig(config);
            GlobalConfigVO globalConfig = new GlobalConfigVO(String.valueOf(i), config, "value", descr[i]);
            list.add(globalConfig);
        }
        return PageResult.success(list, list.size(), 10, 1);
    }
}