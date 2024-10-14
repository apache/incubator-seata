package org.apache.seata.core.auth;

import org.apache.seata.core.protocol.ResultCode;

public class AuthResultBuilder {
    private ResultCode resultCode;
    private String accessToken;
    private String refreshToken;

    public ResultCode getResultCode() {
        return resultCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // set resultCode
    public AuthResultBuilder setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
        return this;
    }

    // set accessToken
    public AuthResultBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    // set refreshToken
    public AuthResultBuilder setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    // build AuthResult
    public AuthResult build() {
        return new AuthResult(this);
    }
}
