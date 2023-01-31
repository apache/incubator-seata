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
package io.seata.integration.tx.api.annotation;

import io.seata.common.util.StringUtils;
import io.seata.rm.tcc.api.BusinessActionContextParameter;

import java.lang.annotation.Annotation;

/**
 * @author leezongjie
 * @date 2022/12/23
 */
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
