/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.example.service.rpc;

import com.alibaba.fescar.example.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Please add the follow VM arguments:
 * <pre>
 *     -Djava.net.preferIPv4Stack=true
 * </pre>
 */
@Service
public class AccountServiceRpc {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    Environment env;

    public void debit(String userId, Integer money) {
        String apiUrl = buildApiUrl("/api/account/debit");

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("userId", userId);
        map.add("money", money.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<String> result = restTemplate.postForEntity(apiUrl, request, String.class);
        if (result.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("debit failure. status=" + result.getStatusCode());
        }
        LOGGER.info("debit done.userId={},money={},result={}", userId, money, result.getBody());
        return;
    }

    private String buildApiUrl(String path) {
        String apiRoot = env.getProperty("app.api.account");
        String apiUrl = apiRoot + path;
        return apiUrl;
    }

}
