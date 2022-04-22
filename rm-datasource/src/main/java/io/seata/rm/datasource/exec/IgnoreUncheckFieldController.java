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
package io.seata.rm.datasource.exec;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationCache;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.seata.common.ConfigurationKeys.TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS;
import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS;

/**
 * Ignore uncheckField Controller
 *
 * @author doubleDimple lovele.cn@gmail.com
 */
public class IgnoreUncheckFieldController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreUncheckFieldController.class);

    private static final IgnoreUncheckFieldConfig LISTENER = new IgnoreUncheckFieldConfig();

    static {
        ConfigurationCache.addConfigListener(TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS, LISTENER);
    }

    private Map<String, Set<String>> ignoreCheckFields;

    /**
     * Instantiates a new Lock retry controller.
     */
    public IgnoreUncheckFieldController() {
        this.ignoreCheckFields = getNoCheckFields();
    }

    public Map<String, Set<String>> getNoCheckFields() {
        return LISTENER.getResultValue();
    }

    static class IgnoreUncheckFieldConfig implements ConfigurationChangeListener {

        private volatile String noCheckFields;

        private volatile Map<String, Set<String>> mapFields = new HashMap<>();

        public IgnoreUncheckFieldConfig() {

            noCheckFields = ConfigurationFactory.getInstance()
                    .getConfig(TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS, DEFAULT_TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS);
            try {
                if (noCheckFields.length() > 0) {
                    Map<String, String> maps = (Map) JSON.parse(noCheckFields);
                    if (maps.size() > 0) {
                        maps.forEach((key, value) -> {
                            mapFields.put(key, Sets.newHashSet(Arrays.asList(value)));
                        });
                    }
                }

            } catch (Exception e) {
                LOGGER.warn("Please confirm whether this configuration:[{}] is correct,error:[{}]", noCheckFields, e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onChangeEvent(ConfigurationChangeEvent event) {
            String dataId = event.getDataId();
            String newValue = event.getNewValue();
            if (TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS.equals(dataId)) {
                noCheckFields = StringUtils.isBlank(newValue) ? DEFAULT_TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS : newValue;
            }
        }

        public Map<String, Set<String>> getResultValue() {
            return mapFields;
        }

    }
}
