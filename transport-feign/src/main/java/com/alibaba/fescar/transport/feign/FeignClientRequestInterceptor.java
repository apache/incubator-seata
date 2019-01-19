package com.alibaba.fescar.transport.feign;

import com.alibaba.fescar.core.context.RootContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Loki
 */
public class FeignClientRequestInterceptor implements RequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeignClientRequestInterceptor.class);


    @Override
    public void apply(RequestTemplate input) {
        String xid = RootContext.getXID();
        if (xid != null) {
            LOGGER.info("set header xid={}", xid);
            input.header(RootContext.KEY_XID, xid);
        } else {
            LOGGER.debug("Cannot inject transaction ID, as the RootContext is null or cannot get the globalTxId.");
        }
    }
}
