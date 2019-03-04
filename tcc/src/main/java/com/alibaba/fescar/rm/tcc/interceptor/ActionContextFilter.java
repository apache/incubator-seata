package com.alibaba.fescar.rm.tcc.interceptor;

import com.alibaba.fescar.rm.tcc.api.BusinessActionContextParameter;

public interface ActionContextFilter {

    public boolean needFilter(BusinessActionContextParameter parameter);

}
