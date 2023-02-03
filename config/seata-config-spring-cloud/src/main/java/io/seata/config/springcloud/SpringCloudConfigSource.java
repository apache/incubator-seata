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

import javax.annotation.Nonnull;

import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.StringUtils;
import io.seata.config.source.RemoteConfigSource;
import org.springframework.context.ApplicationContext;

/**
 * The type SpringCloud config source
 */
public class SpringCloudConfigSource implements RemoteConfigSource {

    private static final String CONFIG_TYPE = "SpringCloudConfig";
    private static final String PREFIX = "seata.";

    private static volatile SpringCloudConfigSource instance;


    public static SpringCloudConfigSource getInstance() {
        if (instance == null) {
            synchronized (SpringCloudConfigSource.class) {
                if (instance == null) {
                    instance = new SpringCloudConfigSource();
                }
            }
        }
        return instance;
    }


    private SpringCloudConfigSource() {
    }


    @Nonnull
    @Override
    public String getName() {
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
