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
package org.apache.seata.integration.tx.api.interceptor;

import org.apache.seata.core.model.BranchType;

import java.util.Map;

/**
 * The two phase business action parameters.
 *
 */
public class TwoPhaseBusinessActionParam {

    private String actionName;

    private Boolean isDelayReport;

    private Boolean useCommonFence;

    private Map<String, Object> businessActionContext;

    private BranchType branchType;

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Boolean getDelayReport() {
        return isDelayReport;
    }

    public void setDelayReport(Boolean delayReport) {
        isDelayReport = delayReport;
    }

    public Boolean getUseCommonFence() {
        return useCommonFence;
    }

    public void setUseCommonFence(Boolean useCommonFence) {
        this.useCommonFence = useCommonFence;
    }

    public Map<String, Object> getBusinessActionContext() {
        return businessActionContext;
    }

    public void setBusinessActionContext(Map<String, Object> businessActionContext) {
        this.businessActionContext = businessActionContext;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }
}
