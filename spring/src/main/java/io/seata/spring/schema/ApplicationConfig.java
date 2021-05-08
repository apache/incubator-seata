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
package io.seata.spring.schema;

import java.util.Set;

/**
 * The type application config
 *
 * @author xingfudeshi@gmail.com
 */
public class ApplicationConfig {
    private String applicationId;
    private String txServiceGroup;
    private String failureHandler;
    private int mode;
    private Set<GtxConfig> gtxConfigs;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    public String getFailureHandler() {
        return failureHandler;
    }

    public void setFailureHandler(String failureHandler) {
        this.failureHandler = failureHandler;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public Set<GtxConfig> getGtxConfigs() {
        return gtxConfigs;
    }

    public void setGtxConfigs(Set<GtxConfig> gtxConfigs) {
        this.gtxConfigs = gtxConfigs;
    }

    @Override
    public String toString() {
        return "ApplicationConfig{" +
            "applicationId='" + applicationId + '\'' +
            ", txServiceGroup='" + txServiceGroup + '\'' +
            ", failureHandler='" + failureHandler + '\'' +
            ", mode=" + mode +
            ", gtxConfigs=" + gtxConfigs +
            '}';
    }


}
