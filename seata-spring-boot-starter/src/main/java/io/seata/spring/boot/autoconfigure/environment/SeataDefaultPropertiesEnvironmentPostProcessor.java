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
package io.seata.spring.boot.autoconfigure.environment;

import io.seata.common.util.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;

import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.core.constants.DefaultValues.DEFAULT_GROUPLIST;
import static io.seata.core.constants.DefaultValues.DEFAULT_TC_CLUSTER;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_SPRING_CLOUD_ALIBABA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration.SPRING_APPLICATION_NAME_KEY;

/**
 * Auto generate seata default properties to the environment
 *
 * @author wang.liang
 */
public class SeataDefaultPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULT_PROPERTY_SOURCE_NAME = "defaultProperties";

    //application-id keys
    private static final String SEATA_APPLICATION_ID_KEY = SEATA_PREFIX + DOT + "application-id";
    private static final String SEATA_SPRING_CLOUD_ALIBABA_APPLICATION_ID_KEY = SEATA_SPRING_CLOUD_ALIBABA_PREFIX + DOT + "application-id";

    //tx-service-group keys
    private static final String SEATA_TX_SERVICE_GROUP_KEY = SEATA_PREFIX + DOT + "tx-service-group";
    private static final String SEATA_SPRING_CLOUD_ALIBABA_TX_SERVICE_GROUP_KEY = SEATA_SPRING_CLOUD_ALIBABA_PREFIX + DOT + "tx-service-group";

    //vgroup-mapping prefix
    private static final String VGROUP_MAPPING_PREFIX = SERVICE_PREFIX + DOT + "vgroup-mapping";

    //grouplist prefix
    private static final String GROUPLIST_PREFIX = SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> defaultProperties = new HashMap<>();

        // copy properties
        this.copyApplicationId(environment, defaultProperties);
        this.copyTxServiceGroup(environment, defaultProperties);

        // generate properties
        this.generateDefaultVgroupMappingAndGrouplist(environment, defaultProperties);

        // add default properties to the property sources
        this.addOrReplace(environment.getPropertySources(), defaultProperties);
    }

    //region copy and generate properties

    private void copyApplicationId(ConfigurableEnvironment environment, Map<String, Object> defaultProperties) {
        this.copyProperties(environment, defaultProperties,
                SPRING_APPLICATION_NAME_KEY, // spring.application.name
                SEATA_APPLICATION_ID_KEY, // seata.application-id
                SEATA_SPRING_CLOUD_ALIBABA_APPLICATION_ID_KEY); // spring.cloud.alibaba.seata.application-id
    }

    private void copyTxServiceGroup(ConfigurableEnvironment environment, Map<String, Object> defaultProperties) {
        this.copyProperties(environment, defaultProperties,
                SEATA_TX_SERVICE_GROUP_KEY, // seata.tx-service-group
                SEATA_SPRING_CLOUD_ALIBABA_TX_SERVICE_GROUP_KEY); // spring.cloud.alibaba.seata.tx-service-group
    }

    private void generateDefaultVgroupMappingAndGrouplist(ConfigurableEnvironment environment, Map<String, Object> defaultProperties) {
        String txServiceGroup = environment.getProperty(SEATA_TX_SERVICE_GROUP_KEY);
        if (StringUtils.isBlank(txServiceGroup)) {
            return;
        }

        // generate the seata.service.vgroup-mapping.{txServiceGroup} value if blank
        String tcClusterKey = VGROUP_MAPPING_PREFIX + DOT + txServiceGroup;
        String tcCluster = environment.getProperty(tcClusterKey);
        if (StringUtils.isBlank(tcCluster)) {
            tcCluster = DEFAULT_TC_CLUSTER;
            defaultProperties.put(tcClusterKey, tcCluster);
        }

        // generate the seata.service.grouplist.{tcClusterKey} value if blank
        String grouplistKey = GROUPLIST_PREFIX + DOT + tcCluster;
        String grouplist = environment.getProperty(grouplistKey);
        if (StringUtils.isBlank(grouplist)) {
            grouplist = DEFAULT_GROUPLIST;
            defaultProperties.put(grouplistKey, grouplist);
        }
    }

    private void copyProperties(ConfigurableEnvironment environment, Map<String, Object> defaultProperties, String... configKeys) {
        if (configKeys == null || configKeys.length < 1) {
            throw new IllegalArgumentException("the configKeys must contains at least 2 keys");
        }

        // get first value that not blank
        String firstKey = null;
        String firstValue = null;
        for (String configKey : configKeys) {
            firstValue = environment.getProperty(configKey);
            if (StringUtils.isNotBlank(firstValue)) {
                firstKey = configKey;
                break;
            }
        }
        if (StringUtils.isBlank(firstKey)) {
            return;
        }

        // copy the first value to other properties
        String value;
        for (String configKey : configKeys) {
            if (configKey.equalsIgnoreCase(firstKey)) {
                continue;
            }
            value = environment.getProperty(configKey);
            if (StringUtils.isBlank(value)) {
                defaultProperties.put(configKey, firstValue);
            }
        }
    }

    //endregion


    // copied from org.springframework.cloud.sleuth.autoconfig.TraceEnvironmentPostProcessor
    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        MapPropertySource target = null;
        if (propertySources.contains(DEFAULT_PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(DEFAULT_PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                target.getSource().putAll(map);
            }
        }
        if (target == null) {
            target = new MapPropertySource(DEFAULT_PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(DEFAULT_PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}