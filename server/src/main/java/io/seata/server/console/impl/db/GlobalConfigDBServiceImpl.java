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
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationFactory;
import io.seata.server.console.config.ConfigurationConsoleProperties;
import io.seata.server.console.service.GlobalConfigService;
import io.seata.server.console.vo.GlobalConfigVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global Config  ServiceImpl
 *
 * @author: Yuzhiqiang
 */
@Service
public class GlobalConfigDBServiceImpl implements GlobalConfigService {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Resource(type = ConfigurationConsoleProperties.class)
    private ConfigurationConsoleProperties properties;


    @Override
    public List<GlobalConfigVO> getConfigList() {
        List<GlobalConfigVO> list = new CopyOnWriteArrayList<>();

        properties.getConfigItems().stream().parallel().forEach(configInfo -> {
            ConfigurationCache.disableCurrent();
            try {
                Integer id = configInfo.getId();
                String config = configInfo.getName();
                String value = CONFIG.getConfig(config);
                if (value == null) {
                    value = configInfo.getDefaultValue();
                }
                String descr = configInfo.getDescr();

                GlobalConfigVO globalConfig = new GlobalConfigVO(id, config, value, descr);
                list.add(globalConfig);
            } finally {
                ConfigurationCache.enableCurrent();
            }
        });

        list.sort(Comparator.comparingInt(GlobalConfigVO::getId));
        return list;
    }
}