package io.seata.commonapi.annotation;

import io.seata.common.util.StringUtils;

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

        } else if (annotation instanceof io.seata.rm.tcc.api.BusinessActionContextParameter ) {
            businessActionContextParameterDesc = new BusinessActionContextParameterDesc();
            io.seata.rm.tcc.api.BusinessActionContextParameter  businessActionContextParameter = (io.seata.rm.tcc.api.BusinessActionContextParameter ) annotation;
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
