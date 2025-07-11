package org.apache.seata.mcp.handler;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class CustomResponseErrorHandler implements ResponseErrorHandler {
    private final ResponseErrorHandler defaultHandler = new DefaultResponseErrorHandler();

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        // ALL STATUS CODES ARE CONSIDERED ERROR FREE
        return false;

        // Ignore 500 errors
        // return !response.getStatusCode().is5xxServerError() && defaultHandler.hasError(response);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        // Empty implementation, making sure no exceptions are thrown
    }
}
