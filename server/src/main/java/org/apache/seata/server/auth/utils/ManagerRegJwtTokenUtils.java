package org.apache.seata.server.auth.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.auth.AuthResult;
import org.apache.seata.core.auth.AuthResultBuilder;
import org.apache.seata.core.protocol.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class ManagerRegJwtTokenUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerRegJwtTokenUtils.class);

    private static final String AUTHORITIES_KEY = "auth";

    private static final String secretKey = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SECURITY_SECRET_KEY);

    private static final String accessTokenValidityInMilliseconds = ConfigurationFactory
            .getInstance().getConfig(ConfigurationKeys.SECURITY_ACCESS_TOKEN_VALID_TIME);

    private static final String refreshTokenValidityInMilliseconds = ConfigurationFactory
            .getInstance().getConfig(ConfigurationKeys.SECURITY_REFRESH_TOKEN_VALID_TIME);


    /**
     * Create access token
     * @return token string
     */
    public String createAccessToken(String username) {
        return createToken(username, accessTokenValidityInMilliseconds);
    }

    /**
     * Create access token
     * @return token string
     */
    public String createRefreshToken(String username) {
        return createToken(username, refreshTokenValidityInMilliseconds);
    }

    private String createToken(String username, String tokenValidityInMilliseconds) {
        /**
         * Current time
         */
        long now = (new Date()).getTime();
        /**
         * Expiration date
         */
        Date expirationDate = new Date(now + Long.parseLong(tokenValidityInMilliseconds));
        /**
         * Key
         */
        SecretKeySpec secretKeySpec = new SecretKeySpec(Decoders.BASE64.decode(secretKey),
                SignatureAlgorithm.HS256.getJcaName());
        /**
         * create token
         */
        return Jwts.builder().setSubject(username).claim(AUTHORITIES_KEY, "").setExpiration(
                expirationDate).signWith(secretKeySpec, SignatureAlgorithm.HS256).compact();
    }

    public AuthResult checkUsernamePassword(String username, String password) {
        if(StringUtils.equals(username, ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SECURITY_USERNME))
                && StringUtils.equals(password, ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SECURITY_PASSWORD))){
            return new AuthResultBuilder()
                    .setResultCode(ResultCode.Success)
                    .setAccessToken(createAccessToken(username))
                    .setRefreshToken(createRefreshToken(username))
                    .build();
        } else {
            return new AuthResultBuilder().setResultCode(ResultCode.Failed).build();
        }
    }

    public AuthResult checkAccessToken(String accessToken) {
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken);
            Claims claims = claimsJws.getBody();
            Date expiration = claims.getExpiration();
            if (System.currentTimeMillis() > expiration.getTime() - Long.parseLong(accessTokenValidityInMilliseconds) / 3) {
                LOGGER.warn("jwt token will be expired, need refresh token");
                return new AuthResultBuilder().setResultCode(ResultCode.AccessTokenNearExpiration).build();
            }
            return new AuthResultBuilder().setResultCode(ResultCode.Success).build();
        } catch (ExpiredJwtException e) {
            LOGGER.warn("jwt token has been expired: " + e);
            return new AuthResultBuilder().setResultCode(ResultCode.AccessTokenExpired).build();
        } catch (Exception e) {
            LOGGER.error("jwt token authentication failed: " + e);
            return new AuthResultBuilder().setResultCode(ResultCode.Failed).build();
        }
    }

    public AuthResult checkRefreshToken(String refreshToken) {
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(refreshToken);
            return new AuthResultBuilder().setResultCode(ResultCode.Success)
                    .setAccessToken(createAccessToken(claimsJws.getBody().getSubject()))
                    .build();
        } catch (ExpiredJwtException e) {
            LOGGER.warn("jwt token has been expired: " + e);
            return new AuthResultBuilder().setResultCode(ResultCode.RefreshTokenExpired).build();
        } catch (Exception e) {
            LOGGER.error("jwt token authentication failed: " + e);
            return new AuthResultBuilder().setResultCode(ResultCode.Failed).build();
        }
    }
}
