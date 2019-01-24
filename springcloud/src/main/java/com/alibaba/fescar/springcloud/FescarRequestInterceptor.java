package com.alibaba.fescar.springcloud;

import com.alibaba.fescar.core.context.RootContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class FescarRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String xid = RootContext.getXID();
        template.header(RootContext.KEY_XID, xid);
    }
}
