package org.apache.seata.common.http;

import java.io.IOException;
import java.util.Map;

public interface HttpExecutor {

    HttpResult doPost(String url, Map<String, String> params, Map<String, String> header, int timeout) throws IOException;

    HttpResult doPost(String url, String body, Map<String, String> header, int timeout) throws IOException;

    HttpResult doGet(String url, Map<String, String> param, Map<String, String> header, int timeout) throws IOException;

    HttpResult doPostJson(String url, String jsonBody, Map<String, String> headers, int timeout) throws IOException;
}
