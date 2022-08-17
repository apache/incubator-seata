/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.console.utils;

import io.seata.console.config.WebSecurityConfig;
import io.seata.console.controller.GlobalSessionServerController;
import io.seata.console.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * generate request url, add token in header and issue request
 *
 * @author: Sher
 */
@Component
public class UrlUtils {
    /**
     * address of server module
     */
    @Value("${seata.server.address}")
    private String address;
    /**
     * port of server module
     */
    @Value("${seata.server.port}")
    private String port;
    @Resource
    RestTemplate restTemplate;
    @Resource
    HttpServletRequest request;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServerController.class);

    /**
     * generate server http request url
     *
     * @param request
     * @return url
     */
    public String generateUrl(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        StringBuilder sb = new StringBuilder();
        //default  http://localhost:7092/server/requestURI?queryString
        sb.append("http://").append(address).append(":").append(port).append("/server").append(requestURI).append("?").append(queryString);
        String url = sb.toString();
        return url;
    }

    /**
     * add token to header
     *
     * @param request
     * @return httpEntity
     */
    public HttpEntity<String> getHttpEntity(HttpServletRequest request) {
        // get bearerToken
        String bearerToken = request.getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        // add bearerToken to header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<String> httpEntity = new HttpEntity(null, httpHeaders);
        return httpEntity;
    }

    /**
     * initiate a request and get pageResult
     *
     * @return pageResult
     */
    public PageResult getPageResult() {
        String url = generateUrl(request);
        // add token to header
        HttpEntity<String> httpEntity = getHttpEntity(request);
        PageResult pageResult = new PageResult();
        try {
            ResponseEntity<PageResult> result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, PageResult.class);
            pageResult = result.getBody();
        } catch (Exception e) {
            LOGGER.error("restTemplate request failed:", e);
        }
        return pageResult;
    }
}
