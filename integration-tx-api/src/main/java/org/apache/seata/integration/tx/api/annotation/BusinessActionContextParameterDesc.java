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
package org.apache.seata.integration.tx.api.annotation;

import java.lang.annotation.Annotation;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;


public class BusinessActionContextParameterDesc {
    private String paramName;
    private int index;
    private boolean isParamInProperty;

    private BusinessActionContextParameterDesc() {
    }

    public static BusinessActionContextParameterDesc createFromBusinessActionContextParameter(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        BusinessActionContextParameterDesc businessActionContextParameterDesc = null;
        if (annotation instanceof BusinessActionContextParameter) {
            businessActionContextParameterDesc = new BusinessActionContextParameterDesc();
            BusinessActionContextParameter businessActionContextParameter = (BusinessActionContextParameter) annotation;
            businessActionContextParameterDesc.setIndex(businessActionContextParameter.index());
            businessActionContextParameterDesc.setParamInProperty(businessActionContextParameter.isParamInProperty());
            businessActionContextParameterDesc.setParamName(StringUtils.isNotBlank(businessActionContextParameter.paramName()) ? businessActionContextParameter.paramName() : businessActionContextParameter.value());
        }
        return businessActionContextParameterDesc;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isParamInProperty() {
        return isParamInProperty;
    }

    public void setParamInProperty(boolean paramInProperty) {
        isParamInProperty = paramInProperty;
    }
}
