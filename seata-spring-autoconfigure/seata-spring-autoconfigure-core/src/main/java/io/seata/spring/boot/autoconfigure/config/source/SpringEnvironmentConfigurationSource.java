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
package io.seata.spring.boot.autoconfigure.config.source;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.util.StringUtils;
import io.seata.config.source.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static io.seata.common.util.StringFormatUtils.DOT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SEATA_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVICE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_GROUPLIST;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SPECIAL_KEY_SERVICE;

/**
 * The type SpringEnvironmentConfigurationSource.
 *
 * @author wang.liang
 */
public class SpringEnvironmentConfigurationSource implements ConfigurationSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringEnvironmentConfigurationSource.class);


    @Override
    public String getLatestConfig(String rawDataId, long timeoutMills) {
        ConfigurableEnvironment environment = (ConfigurableEnvironment)ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        if (environment == null) {
            throw new ShouldNeverHappenException("The environment should never set to the ObjectHolder.");
        }

        // Splice the prefix 'seata.'
        String dataId = splicePrefixSeataDot(rawDataId);
        // hump to line
        String dataIdLineFormat = StringUtils.hump2Line(dataId);

        // 1. get by dataIdLineFormat
        String value = environment.getProperty(dataIdLineFormat);
        if (StringUtils.isNotBlank(value)) {
            this.log(rawDataId, value, dataIdLineFormat);
            return value;
        }

        // 2. get by dataId
        if (!dataId.equals(dataIdLineFormat)) {
            value = environment.getProperty(dataId);
            if (StringUtils.isNotBlank(value)) {
                this.log(rawDataId, value, dataId);
                return value;
            }
        }

        // 3. If the rawDataId format is 'service.{txServiceGroup}.grouplist', change and get again
        if (this.isGrouplistDataId(rawDataId)) {
            String grouplistDataId = this.changeGrouplistDataIdFormat(rawDataId);
            value = environment.getProperty(grouplistDataId);
            if (StringUtils.isNotBlank(value)) {
                this.log(rawDataId, value, grouplistDataId);
                return value;
            }
        }

        return null;
    }

    @Override
    public String getTypeName() {
        return "spring-environment";
    }

    /**
     * Splice the prefix 'seata.'
     *
     * @param rawDataId the rawDataId
     * @return the real dataId for spring environment
     */
    private String splicePrefixSeataDot(String rawDataId) {
        if (rawDataId.startsWith(SEATA_PREFIX + DOT)) {
            return rawDataId;
        }

        return SEATA_PREFIX + DOT + rawDataId;
    }

    /**
     * Whether the format is 'service.{txServiceGroup}.grouplist'
     *
     * @param rawDataId the rawDataId
     * @return the boolean
     */
    private boolean isGrouplistDataId(String rawDataId) {
        return rawDataId.startsWith(SPECIAL_KEY_SERVICE + DOT) && rawDataId.endsWith(DOT + SPECIAL_KEY_GROUPLIST);
    }

    /**
     * Change the format of 'service.{txServiceGroup}.grouplist' to 'seata.service.grouplist.{txServiceGroup}'
     *
     * @param rawDataId the rawDataId
     * @return the real dataId
     */
    private String changeGrouplistDataIdFormat(final String rawDataId) {
        String txServiceGroup = org.apache.commons.lang.StringUtils.removeStart(
                org.apache.commons.lang.StringUtils.removeEnd(rawDataId, DOT + SPECIAL_KEY_GROUPLIST),
                SPECIAL_KEY_SERVICE + DOT
        );

        return SERVICE_PREFIX + DOT + SPECIAL_KEY_GROUPLIST + DOT + txServiceGroup;
    }

    private void log(String rawDataId, String value, String dataId) {
        LOGGER.debug("Get config '{}' value '{}' from spring environment by dataId '{}'",
                rawDataId, value, dataId);
    }
}