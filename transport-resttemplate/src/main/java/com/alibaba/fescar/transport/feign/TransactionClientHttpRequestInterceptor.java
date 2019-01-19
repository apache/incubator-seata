package com.alibaba.fescar.transport.feign;

import com.alibaba.fescar.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * @author Loki
 */
public class TransactionClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionClientHttpRequestInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String xid = RootContext.getXID();
        if (xid != null) {
            LOGGER.info("set header xid={}", xid);
            request.getHeaders().add(RootContext.KEY_XID, xid);
        } else {
            LOGGER.debug("Cannot inject transaction ID, as the RootContext is null or cannot get the globalTxId.");
        }
        return execution.execute(request, body);
    }
}
