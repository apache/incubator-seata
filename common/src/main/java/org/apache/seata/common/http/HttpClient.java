package org.apache.seata.common.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface HttpClient {

    CompletableFuture<HttpResponseWrapper> doPost(String url, Map<String, String> params, Map<String, String> header, int timeout) throws IOException;

    CompletableFuture<HttpResponseWrapper> doPost(String url, String body, Map<String, String> header, int timeout) throws IOException;

    CompletableFuture<HttpResponseWrapper> doGet(String url, Map<String, String> param, Map<String, String> header, int timeout) throws IOException;

    CompletableFuture<HttpResponseWrapper> doPostJson(String url, String jsonBody, Map<String, String> headers, int timeout) throws IOException;
}
