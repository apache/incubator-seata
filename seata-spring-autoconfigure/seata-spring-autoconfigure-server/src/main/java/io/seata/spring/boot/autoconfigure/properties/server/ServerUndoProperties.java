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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_UNDO_LOG_DELETE_PERIOD;
import static io.seata.core.protocol.transaction.UndoLogDeleteRequest.DEFAULT_SAVE_DAYS;
import static io.seata.spring.boot.autoconfigure.StarterConstants.SERVER_UNDO_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = SERVER_UNDO_PREFIX)
public class ServerUndoProperties {
    private int logSaveDays = DEFAULT_SAVE_DAYS;
    private long logDeletePeriod = DEFAULT_UNDO_LOG_DELETE_PERIOD;

    public int getLogSaveDays() {
        return logSaveDays;
    }

    public ServerUndoProperties setLogSaveDays(int logSaveDays) {
        this.logSaveDays = logSaveDays;
        return this;
    }

    public long getLogDeletePeriod() {
        return logDeletePeriod;
    }

    public ServerUndoProperties setLogDeletePeriod(long logDeletePeriod) {
        this.logDeletePeriod = logDeletePeriod;
        return this;
    }
}
