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
package io.seata.config.springcloud;

import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.StringUtils;
import io.seata.config.source.RemoteConfigurationSource;
import org.springframework.context.ApplicationContext;

/**
 * The type SpringCloud configuration source
 */
public class SpringCloudConfigurationSource implements RemoteConfigurationSource {

    private static final String CONFIG_TYPE = "SpringCloudConfig";
    private static final String PREFIX = "seata.";

    private static volatile SpringCloudConfigurationSource instance;


    public static SpringCloudConfigurationSource getInstance() {
        if (instance == null) {
            synchronized (SpringCloudConfigurationSource.class) {
                if (instance == null) {
                    instance = new SpringCloudConfigurationSource();
                }
            }
        }
        return instance;
    }


    private SpringCloudConfigurationSource() {
    }


    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    @Override
    public String getLatestConfig(String dataId, long timeoutMills) {
        ApplicationContext applicationContext = ObjectHolder.INSTANCE.getObject(ApplicationContext.class);
        if (applicationContext == null || applicationContext.getEnvironment() == null) {
            return null;
        }
        String conf = applicationContext.getEnvironment().getProperty(PREFIX + dataId);
        return StringUtils.isNotBlank(conf) ? conf : null;
    }
}
