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
package io.seata.rm.datasource.undo;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

/**
 * The type Undo log parser factory.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public class UndoLogParserFactory {

    private static class SingletonHolder {
        private static final UndoLogParser INSTANCE = 
                EnhancedServiceLoader.load(UndoLogParser.class, ConfigurationFactory.getInstance()
                        .getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_SERIALIZATION, "fastjson"));
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static UndoLogParser getInstance() {
        return SingletonHolder.INSTANCE;
    }

}
