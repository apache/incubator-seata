/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.apache.seata.sqlparser.SqlParserType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_RETRY_COUNT;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_SAGA_JSON_PARSER;
import static org.apache.seata.common.DefaultValues.DEFAULT_TABLE_META_CHECKER_INTERVAL;
import static org.apache.seata.common.DefaultValues.TCC_ACTION_INTERCEPTOR_ORDER;
import static org.apache.seata.common.DefaultValues.DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_APPLICATION_DATA_SIZE_LIMIT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;


@Component
@ConfigurationProperties(prefix = CLIENT_RM_PREFIX)
public class RmProperties {
    private int asyncCommitBufferLimit = DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
    private int reportRetryCount = DEFAULT_CLIENT_REPORT_RETRY_COUNT;
    private boolean tableMetaCheckEnable = DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE;
    private long tableMetaCheckerInterval = DEFAULT_TABLE_META_CHECKER_INTERVAL;
    private boolean reportSuccessEnable = DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
    private boolean sagaBranchRegisterEnable = DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
    private String sagaJsonParser = DEFAULT_SAGA_JSON_PARSER;
    private boolean sagaRetryPersistModeUpdate = DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE;
    private boolean sagaCompensatePersistModeUpdate = DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE;
    private int tccActionInterceptorOrder = TCC_ACTION_INTERCEPTOR_ORDER;
    private int branchExecutionTimeoutXA = DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT;
    private int connectionTwoPhaseHoldTimeoutXA = DEFAULT_XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT;
    private String sqlParserType = SqlParserType.SQL_PARSER_TYPE_DRUID;

    private Boolean applicationDataLimitCheck = false;
    private Integer applicationDataLimit = DEFAULT_APPLICATION_DATA_SIZE_LIMIT;

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


    public long getTableMetaCheckerInterval() {
        return tableMetaCheckerInterval;
    }

    public void setTableMetaCheckerInterval(long tableMetaCheckerInterval) {
        this.tableMetaCheckerInterval = tableMetaCheckerInterval;
    }

    public boolean isSagaRetryPersistModeUpdate() {
        return sagaRetryPersistModeUpdate;
    }

    public void setSagaRetryPersistModeUpdate(boolean sagaRetryPersistModeUpdate) {
        this.sagaRetryPersistModeUpdate = sagaRetryPersistModeUpdate;
    }

    public boolean isSagaCompensatePersistModeUpdate() {
        return sagaCompensatePersistModeUpdate;
    }

    public void setSagaCompensatePersistModeUpdate(boolean sagaCompensatePersistModeUpdate) {
        this.sagaCompensatePersistModeUpdate = sagaCompensatePersistModeUpdate;
    }

    public int getTccActionInterceptorOrder() {
        return tccActionInterceptorOrder;
    }

    public RmProperties setTccActionInterceptorOrder(int tccActionInterceptorOrder) {
        this.tccActionInterceptorOrder = tccActionInterceptorOrder;
        return this;
    }

    public String getSqlParserType() {
        return sqlParserType;
    }

    public RmProperties setSqlParserType(String sqlParserType) {
        this.sqlParserType = sqlParserType;
        return this;
    }

    public int getBranchExecutionTimeoutXA() {
        return branchExecutionTimeoutXA;
    }

    public void setBranchExecutionTimeoutXA(int branchExecutionTimeoutXA) {
        this.branchExecutionTimeoutXA = branchExecutionTimeoutXA;
    }

    public int getConnectionTwoPhaseHoldTimeoutXA() {
        return connectionTwoPhaseHoldTimeoutXA;
    }

    public void setConnectionTwoPhaseHoldTimeoutXA(int connectionTwoPhaseHoldTimeoutXA) {
        this.connectionTwoPhaseHoldTimeoutXA = connectionTwoPhaseHoldTimeoutXA;
    }

    public Boolean getApplicationDataLimitCheck() {
        return applicationDataLimitCheck;
    }

    public void setApplicationDataLimitCheck(Boolean applicationDataLimitCheck) {
        this.applicationDataLimitCheck = applicationDataLimitCheck;
    }

    public Integer getApplicationDataLimit() {
        return applicationDataLimit;
    }

    public void setApplicationDataLimit(Integer applicationDataLimit) {
        this.applicationDataLimit = applicationDataLimit;
    }
}
