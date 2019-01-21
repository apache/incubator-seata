package com.alibaba.fescar.example.interceptor;

import com.alibaba.fescar.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class TxRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    static final Logger LOGGER = LoggerFactory.getLogger(TxRestTemplateInterceptor.class);

    private static final String HEADER_TRANSACTION = "x-fescar-xid";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        HttpHeaders headers = request.getHeaders();

        //加入xid到header中
        String xid = RootContext.getXID();
        if (StringUtils.hasText(xid)) {
            headers.add(HEADER_TRANSACTION, xid);
            LOGGER.debug("add header. name={}, value={}", HEADER_TRANSACTION, xid);
        }

        return execution.execute(request, body);
    }
}
