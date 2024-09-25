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
package org.apache.seata.server.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.apache.seata.common.result.Code;
import org.apache.seata.common.result.SingleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.List;

/**
 * Jwt token tool
 *
 */
@Component("clusterJwtTokenUtils")
public class RaftRegJwtTokenUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftRegJwtTokenUtils.class);

    private static final String AUTHORITIES_KEY = "auth";

    /**
     * secret key
     */
    @Value("${seata.security.secretKey}")
    private String secretKey;

    /**
     * Access token validity time(ms)
     */
    @Value("${seata.security.accessTokenValidityInMilliseconds}")
    private long accessTokenValidityInMilliseconds;

    /**
     * Refresh token validity time(ms)
     */
    @Value("${seata.security.refreshTokenValidityInMilliseconds}")
    private long refreshTokenValidityInMilliseconds;

    /**
     * Create access token
     *
     * @param authentication auth info
     * @return token string
     */
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, accessTokenValidityInMilliseconds);
    }

    /**
     * Create refresh token
     *
     * @param authentication auth info
     * @return token string
     */
    public String createRefreshToken(Authentication authentication) {
        return createToken(authentication, refreshTokenValidityInMilliseconds);
    }

    /**
     * Create token
     * @param authentication auth info
     * @param tokenValidityInMilliseconds token validity time in milliseconds
     * @return token string
     */
    private String createToken(Authentication authentication, long tokenValidityInMilliseconds) {
        /**
         * Current time
         */
        long now = (new Date()).getTime();
        /**
         * Expiration date
         */
        Date expirationDate = new Date(now + tokenValidityInMilliseconds);
        /**
         * Key
         */
        SecretKeySpec secretKeySpec = new SecretKeySpec(Decoders.BASE64.decode(secretKey),
                SignatureAlgorithm.HS256.getJcaName());
        /**
         * create token
         */
        return Jwts.builder().setSubject(authentication.getName()).claim(AUTHORITIES_KEY, "").setExpiration(
                expirationDate).signWith(secretKeySpec, SignatureAlgorithm.HS256).compact();
    }

    /**
     * validate access token
     *
     * @param token token
     * @return validate result
     */
    public SingleResult validateAccessToken(String token) {
        try {
            /**
             *  parse the payload of access token
             */
            Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
            List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(
                    (String)claims.get(AUTHORITIES_KEY));
            User principal = new User(claims.getSubject(), "", authorities);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(principal, "", authorities);
            if (System.currentTimeMillis() > claims.getExpiration().getTime() - accessTokenValidityInMilliseconds / 3) {
                LOGGER.warn("jwt token will be expired, need refresh token");
                return new SingleResult<>(Code.ACCESS_TOKEN_NEAR_EXPIRATION, authenticationToken);
            }
            return new SingleResult<>(Code.SUCCESS, authenticationToken);
        } catch (ExpiredJwtException e) {
            LOGGER.warn("Expired JWT token.");
            LOGGER.trace("Expired JWT token trace: {}", e);
            return new SingleResult<>(Code.ACCESS_TOKEN_EXPIRED);
        } catch (Exception e) {
            LOGGER.warn("Unsupported JWT token.");
            LOGGER.trace("Unsupported JWT token trace: {}", e);
            return new SingleResult<>(Code.CHECK_TOKEN_FAILED);
        }
    }

    /**
     * validate refresh token
     *
     * @param token token
     * @return validate result
     */
    public SingleResult validateRefreshToken(String token) {
        try {
            /**
             *  parse the payload of refresh token
             */
            Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
            List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(
                    (String)claims.get(AUTHORITIES_KEY));
            User principal = new User(claims.getSubject(), "", authorities);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(principal, "", authorities);
            return new SingleResult<>(Code.SUCCESS, authenticationToken);
        } catch (ExpiredJwtException e) {
            LOGGER.warn("Expired JWT token.");
            LOGGER.trace("Expired JWT token trace: {}", e);
            return new SingleResult<>(Code.REFRESH_TOKEN_EXPIRED);
        } catch (Exception e) {
            LOGGER.warn("Unsupported JWT token.");
            LOGGER.trace("Unsupported JWT token trace: {}", e);
            return new SingleResult<>(Code.CHECK_TOKEN_FAILED);
        }
    }

}
