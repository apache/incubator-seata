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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
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
public class IgnoreUncheckFieldController implements ConfigurationChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreUncheckFieldController.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private String noCheckFields;

    private static Map<String, Set<String>> mapFields = new HashMap<>();

    private static final class IgnoreUncheckFieldControllerHolder {
        private static IgnoreUncheckFieldController instance = new IgnoreUncheckFieldController();
    }

    public static IgnoreUncheckFieldController getInstance() {
        return IgnoreUncheckFieldControllerHolder.instance;
    }

    public Map<String, Set<String>> getNoCheckFields() {
        return mapFields;
    }

    @Override
    public void onChangeEvent(ConfigurationChangeEvent event) {
        String dataId = event.getDataId();
        String newValue = event.getNewValue();
        if (TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS.equals(dataId)) {
            noCheckFields = StringUtils.isBlank(newValue) ? DEFAULT_TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS : newValue;
        }
        try {
            if (noCheckFields.length() > 0) {
                Map<String, String> maps = mapper.readValue(noCheckFields, Map.class);
                if (maps.size() > 0) {
                    maps.forEach((key, value) -> {
                        mapFields.put(key, Sets.newHashSet(Arrays.asList(value)));
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Please confirm whether this configuration:[{}] is correct,error:[{}]", noCheckFields,
                e.getMessage());
            e.printStackTrace();
        }
    }

}
