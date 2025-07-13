package org.apache.seata.mcp.service;

import org.springframework.http.HttpHeaders;

import java.util.Map;

public interface MCPRPCService {
    public String postCallTC(String path, HttpHeaders headers, Object... args);

    public String getCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers);

    public String deleteCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers);

    public String putCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers);
}
