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
package io.seata.console.controller;

import io.seata.console.config.WebSecurityConfig;
import io.seata.console.result.GlobalSessionVO;
import io.seata.console.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @description: inner server http request of global session
 * @author: Sher
 */


@RestController
@RequestMapping("api/v1/console/globalSession")
public class GlobalSessionServerController {
    @Autowired
    RestTemplate restTemplate;
    @Resource
    HttpServletRequest request;
//    @Value("${seata.server.port}")

    @Value("${seata.server.port}")
    private Integer port;
//    private Integer port;

    @Value("${seata.server.address}")
    private String address;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServerController.class);


    @RequestMapping("query")
    PageResult<GlobalSessionVO> queryByXid() {
        // generate innner http request url
        String url = generateUrl(request);

        // add token to header
        HttpEntity<String> httpEntity = getHttpEntity(request);

        PageResult response = new PageResult();


        try {
            ResponseEntity<PageResult> result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, PageResult.class);
            response = result.getBody();
        } catch (Exception e) {
            LOGGER.error("Server request error：", e);
        }

        return response;
    }

    // generate server http request url
    private String generateUrl(HttpServletRequest request) {
        String requesturl = request.getRequestURI();
        String queryString = request.getQueryString();
        // http://localhost:7091/server//api/v1/console/globalLock
        String url = "http://" + address + ":" + port + "/server" + requesturl + "?" + queryString;
        return url;
    }

    // add token to header
    private HttpEntity<String> getHttpEntity(HttpServletRequest request) {
        // get bearerToken
        String bearerToken = request.getHeader(WebSecurityConfig.AUTHORIZATION_HEADER);
        // add beartoken to header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<String> httpEntity = new HttpEntity(null, httpHeaders);
        return httpEntity;
    }

}
