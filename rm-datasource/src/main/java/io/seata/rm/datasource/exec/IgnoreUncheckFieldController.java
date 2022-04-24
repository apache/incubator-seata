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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.rm.datasource.sql.struct.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.ConfigurationKeys.TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS;

/**
 * Ignore uncheckField Controller
 *
 * @author doubleDimple lovele.cn@gmail.com
 */
public class IgnoreUncheckFieldController implements ConfigurationChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreUncheckFieldController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String noCheckFields;

    private static volatile Map<String, Set<String>> mapFields = new HashMap<>();

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
            if (StringUtils.isBlank(newValue)) {
                if (CollectionUtils.isNotEmpty(mapFields)) {
                    mapFields.clear();
                }
                return;
            }
            noCheckFields = newValue;
        }
        mapFields = new HashMap<>();
        try {
            if (StringUtils.isNotBlank(noCheckFields)) {
                final List<Map<String, String>> mapList = objectMapper.readValue(noCheckFields, List.class);
                if (CollectionUtils.isNotEmpty(mapList)) {
                    for (Map<String, String> stringStringMap : mapList) {
                        stringStringMap.forEach((key, value) -> {
                            Set<String> stringSet = new HashSet<>();
                            (Arrays.stream(value.split(","))).forEach(e -> stringSet.add(e));
                            mapFields.put(key, stringSet);
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Please confirm whether this configuration:[{}] is correct,error:[{}]", noCheckFields,
                    e.getMessage());
            e.printStackTrace();
        }
    }

    public static void doIgnoreCheckColumns(Set<String> columnNames, StringJoiner selectSQLJoin, String tableName) {

        Set<String> columns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        columns.addAll(columnNames);

        Map<String, Set<String>> ignoreCheckFields = IgnoreUncheckFieldController.getInstance().getNoCheckFields();
        if (CollectionUtils.isNotEmpty(ignoreCheckFields)) {
            if (ignoreCheckFields.containsKey(tableName)) {
                columnNames.removeAll(ignoreCheckFields.get(tableName));
                if (columnNames.size() > 0) {
                    columns = columnNames;
                } else {
                    LOGGER.warn(
                            "the tableName:[{}] config:[{}] columns cause all to be removed so this configuration is no loner in effect",
                            tableName, TRANSACTION_UNDO_IGNORE_NOCHECK_COLUMNS);
                }
            }
        }

        columns.forEach(selectSQLJoin::add);
    }

    public static Boolean checkIgnoreFields(String tableName, Field newField) {

        Map<String, Set<String>> ignoreCheckFields = IgnoreUncheckFieldController.getInstance().getNoCheckFields();

        if (CollectionUtils.isNotEmpty(ignoreCheckFields)) {
            if (ignoreCheckFields.containsKey(tableName)) {
                Set<String> columns = ignoreCheckFields.get(tableName);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.info("tableName:[{}] ignore uncheck column:[{}] ", tableName, columns);
                }
                if (columns.contains(newField.getName())) {
                    return Boolean.TRUE;
                } else {
                    return Boolean.FALSE;
                }
            }
        }
        return Boolean.FALSE;
    }

}
