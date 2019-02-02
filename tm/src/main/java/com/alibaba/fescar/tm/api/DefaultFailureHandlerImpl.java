/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.tm.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default failure handler.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: feats -all
 * @DateTime: 2019 /1/8 7:27 PM
 * @FileName: DefaultFailureHandlerImpl
 * @Description:
 */
public class DefaultFailureHandlerImpl implements FailureHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFailureHandlerImpl.class);

    @Override
    public void onBeginFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to begin transaction. ", cause);
    }

    @Override
    public void onCommitFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to commit transaction[" + tx.getXid() + "]", cause);
    }

    @Override
    public void onRollbackFailure(GlobalTransaction tx, Throwable cause) {
        LOGGER.warn("Failed to rollback transaction[" + tx.getXid() + "]", cause);
    }
}
