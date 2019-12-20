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
package io.seata.spring.boot.autoconfigure.properties.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CLIENT_PREFIX)
public class ClientProperties {
    private int rmAsyncCommitBufferLimit = 10000;
    private int rmReportRetryCount = 5;
    private int tmCommitRetryCount = 5;
    private int tmRollbackRetryCount = 5;
    private boolean rmTableMetaCheckEnable = false;
    private boolean rmReportSuccessEnable = true;

    public int getRmAsyncCommitBufferLimit() {
        return rmAsyncCommitBufferLimit;
    }

    public ClientProperties setRmAsyncCommitBufferLimit(int rmAsyncCommitBufferLimit) {
        this.rmAsyncCommitBufferLimit = rmAsyncCommitBufferLimit;
        return this;
    }

    public int getRmReportRetryCount() {
        return rmReportRetryCount;
    }

    public ClientProperties setRmReportRetryCount(int rmReportRetryCount) {
        this.rmReportRetryCount = rmReportRetryCount;
        return this;
    }

    public int getTmCommitRetryCount() {
        return tmCommitRetryCount;
    }

    public ClientProperties setTmCommitRetryCount(int tmCommitRetryCount) {
        this.tmCommitRetryCount = tmCommitRetryCount;
        return this;
    }

    public int getTmRollbackRetryCount() {
        return tmRollbackRetryCount;
    }

    public ClientProperties setTmRollbackRetryCount(int tmRollbackRetryCount) {
        this.tmRollbackRetryCount = tmRollbackRetryCount;
        return this;
    }

    public boolean isRmTableMetaCheckEnable() {
        return rmTableMetaCheckEnable;
    }

    public ClientProperties setRmTableMetaCheckEnable(boolean rmTableMetaCheckEnable) {
        this.rmTableMetaCheckEnable = rmTableMetaCheckEnable;
        return this;
    }

    public boolean isRmReportSuccessEnable() {
        return rmReportSuccessEnable;
    }

    public void setRmReportSuccessEnable(boolean rmReportSuccessEnable) {
        this.rmReportSuccessEnable = rmReportSuccessEnable;
    }
}
