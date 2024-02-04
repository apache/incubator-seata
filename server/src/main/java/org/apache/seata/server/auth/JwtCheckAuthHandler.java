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
package org.apache.seata.server.auth;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.seata.common.exception.RetryableException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.auth.JwtAuthManager;
import org.apache.seata.core.protocol.AbstractIdentifyRequest;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.HashMap;

@LoadLevel(name = "jwtCheckAuthHandler", order = 1)
public class JwtCheckAuthHandler extends AbstractCheckAuthHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtCheckAuthHandler.class);

    private static final String AUTHORITIES_KEY = "auth";

    private static final String TOKEN = "token";

    private JwtAuthManager authManager = JwtAuthManager.getInstance();

    @Override
    public boolean doRegTransactionManagerCheck(RegisterTMRequest request) throws RetryableException {
        return checkAuthData(request.getExtraData());
    }

    @Override
    public boolean doRegResourceManagerCheck(RegisterRMRequest request) throws RetryableException {
        return checkAuthData(request.getExtraData());
    }

    @Override
    public boolean needRefreshToken(AbstractIdentifyRequest abstractIdentifyRequest) {
        try {
            if (!checkAuthData(abstractIdentifyRequest.getExtraData())) {
                return false;
            }
        } catch (RetryableException e) {
            LOGGER.warn("auth failed!");
            return false;
        }

        HashMap<String, String> authDataMap = JwtAuthManager.convertToHashMap(abstractIdentifyRequest.getExtraData());
        // 1.if use username/password for authentication, need refresh token.
        if (!authDataMap.containsKey(TOKEN)) {
            return true;
        } else {
            // 2.if token will be expired, need refresh token.
            try {
                String accessToken = authDataMap.get(TOKEN);
                String secretKey = ConfigurationFactory.getInstance().getConfig("security.secretKey");
                String tokenValidWindow = ConfigurationFactory.getInstance().getConfig("security.tokenValidityInMilliseconds");
                Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken);
                Claims claims = claimsJws.getBody();
                Date expiration = claims.getExpiration();
                if (System.currentTimeMillis() > expiration.getTime() - Long.parseLong(tokenValidWindow) / 3) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.error("jwt token authentication failed: " + e);
            }
        }
        return false;
    }

    @Override
    public String refreshToken(AbstractIdentifyRequest abstractIdentifyRequest) {
        String subject;
        String secretKey = ConfigurationFactory.getInstance().getConfig("security.secretKey");
        String expirationMillis = ConfigurationFactory.getInstance().getConfig("security.tokenValidityInMilliseconds");
        if (authManager.getUsername() != null) {
            subject = authManager.getUsername();
        } else {
            String accessToken = authManager.getToken();
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken);
            Claims claims = claimsJws.getBody();
            subject = claims.getSubject();
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(Decoders.BASE64.decode(secretKey),
            SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
            .setSubject(subject)
            .claim(AUTHORITIES_KEY, "")
            .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(expirationMillis)))
            .signWith(secretKeySpec, SignatureAlgorithm.HS256)
            .compact();
    }

    private boolean checkAuthData(String extraData) throws RetryableException {
        if (null == extraData) {
            return false;
        }
        HashMap<String, String> authData = JwtAuthManager.convertToHashMap(extraData);
        // 1.check username/password
        String username = authData.get("username");
        String password = authData.get("password");
        if (null != username && null != password
            && StringUtils.equals(username, ConfigurationFactory.getInstance().getConfig("security.username"))
            && StringUtils.equals(password, ConfigurationFactory.getInstance().getConfig("security.password"))) {
            authManager.setUsername(username);
            authManager.setPassword(password);
            return true;
        } else if (authData.get(TOKEN) != null) {
            // 2.check token
            try {
                String accessToken = authData.get(TOKEN);
                String secretKey = ConfigurationFactory.getInstance().getConfig("security.secretKey");
                Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken);
                authManager.setAccessToken(accessToken);
                return true;
            } catch (ExpiredJwtException e) {
                LOGGER.warn("jwt token has been expired: " + e);
                throw new RetryableException();
            } catch (Exception e) {
                LOGGER.error("jwt token authentication failed: " + e);
            }
        }
        return false;
    }

}
