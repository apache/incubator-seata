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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_RETRY_COUNT;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_SAGA_JSON_PARSER;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CLIENT_RM_PREFIX)
public class RmProperties {
    private int asyncCommitBufferLimit = DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
    private int reportRetryCount = DEFAULT_CLIENT_REPORT_RETRY_COUNT;
    private boolean tableMetaCheckEnable = DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE;
    private boolean reportSuccessEnable = DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
    private boolean sagaBranchRegisterEnable = DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
    private String sagaJsonParser = DEFAULT_SAGA_JSON_PARSER;

    public int getAsyncCommitBufferLimit() {
        return asyncCommitBufferLimit;
    }

    public RmProperties setAsyncCommitBufferLimit(int asyncCommitBufferLimit) {
        this.asyncCommitBufferLimit = asyncCommitBufferLimit;
        return this;
    }

    public int getReportRetryCount() {
        return reportRetryCount;
    }

    public RmProperties setReportRetryCount(int reportRetryCount) {
        this.reportRetryCount = reportRetryCount;
        return this;
    }

    public boolean isTableMetaCheckEnable() {
        return tableMetaCheckEnable;
    }

    public RmProperties setTableMetaCheckEnable(boolean tableMetaCheckEnable) {
        this.tableMetaCheckEnable = tableMetaCheckEnable;
        return this;
    }

    public boolean isReportSuccessEnable() {
        return reportSuccessEnable;
    }

    public RmProperties setReportSuccessEnable(boolean reportSuccessEnable) {
        this.reportSuccessEnable = reportSuccessEnable;
        return this;
    }

    public boolean isSagaBranchRegisterEnable() {
        return sagaBranchRegisterEnable;
    }

    public void setSagaBranchRegisterEnable(boolean sagaBranchRegisterEnable) {
        this.sagaBranchRegisterEnable = sagaBranchRegisterEnable;
    }

    public String getSagaJsonParser() {
        return sagaJsonParser;
    }

    public void setSagaJsonParser(String sagaJsonParser) {
        this.sagaJsonParser = sagaJsonParser;
    }
}
