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
package io.seata.spring.boot.autoconfigure.properties;

import io.seata.spring.boot.autoconfigure.StarterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The type Spring cloud alibaba configuration.
 *
 * @author slievrly
 */
@ConfigurationProperties(prefix = StarterConstants.SEATA_SPRING_CLOUD_ALIBABA_PREFIX)
public class SpringCloudAlibabaConfiguration implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCloudAlibabaConfiguration.class);
    private static final String SPRING_APPLICATION_NAME_KEY = "spring.application.name";
    private static final String DEFAULT_SPRING_CLOUD_SERVICE_GROUP_POSTFIX = "-seata-service-group";
    private String applicationId;
    private String txServiceGroup;
    private ApplicationContext applicationContext;

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        if (null == applicationId) {
            applicationId = applicationContext.getEnvironment().getProperty(SPRING_APPLICATION_NAME_KEY);
        }
        return applicationId;
    }

    /**
     * Gets tx service group.
     *
     * @return the tx service group
     */
    public String getTxServiceGroup() {
        if (null == txServiceGroup) {
            String applicationId = getApplicationId();
            if (null == applicationId) {
                LOGGER.warn("{} is null, please set its value", SPRING_APPLICATION_NAME_KEY);
            }
            txServiceGroup = applicationId + DEFAULT_SPRING_CLOUD_SERVICE_GROUP_POSTFIX;
        }
        return txServiceGroup;
    }

    /**
     * Sets tx service group.
     *
     * @param txServiceGroup the tx service group
     */
    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
