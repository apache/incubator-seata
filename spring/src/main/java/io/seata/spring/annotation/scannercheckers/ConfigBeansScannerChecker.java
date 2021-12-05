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
package io.seata.spring.annotation.scannercheckers;

import javax.annotation.Nullable;

import io.seata.common.loader.LoadLevel;
import io.seata.spring.annotation.ScannerChecker;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Config scanner checker.
 *
 * @author wang.liang
 */
@LoadLevel(name = "ConfigBeans", order = 150)
public class ConfigBeansScannerChecker implements ScannerChecker {

    @Override
    public boolean check(Object bean, String beanName, @Nullable ConfigurableListableBeanFactory beanFactory) throws Exception {
        if (beanName != null && (beanName.endsWith("Configuration") || beanName.endsWith("Properties") || beanName.endsWith("Config"))) {
            // do not scan the config beans
            return false;
        }

        return true;
    }
}
