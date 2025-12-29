/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ModifyConfirmServiceImpl implements ModifyConfirmService {

    private final JwtTokenUtils jwtTokenUtils;

    private static final long MODIFY_TOKEN_VALIDITY_IN_MILLISECONDS = 180_000;

    @Value("${seata.security.secretKey}")
    private String secretKey;

    public ModifyConfirmServiceImpl(JwtTokenUtils jwtTokenUtils) {
        this.jwtTokenUtils = jwtTokenUtils;
    }

    @Override
    public Map<String, String> confirmAndGetKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        long now = (new Date()).getTime();
        Date expirationDate = new Date(now + MODIFY_TOKEN_VALIDITY_IN_MILLISECONDS);
        SecretKeySpec secretKeySpec =
                new SecretKeySpec(Decoders.BASE64.decode(secretKey), SignatureAlgorithm.HS256.getJcaName());
        String key = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("modify", "")
                .setExpiration(expirationDate)
                .signWith(secretKeySpec, SignatureAlgorithm.HS256)
                .compact();
        Map<String, String> map = new HashMap<>();
        map.put("modify_key", key);
        map.put(
                "Important!!!",
                "You need to repeat the content to be modified by the user and get confirmation from the user before you can continue to call the modification tool");
        return map;
    }

    @Override
    public Boolean isValidKey(String key) {
        return jwtTokenUtils.validateToken(key);
    }
}
