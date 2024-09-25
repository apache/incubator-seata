package org.apache.seata.core.auth;

import org.apache.seata.core.protocol.ResultCode;

public class AuthResult {
    private ResultCode resultCode;

    private String accessToken;

    private String refreshToken;

    public AuthResult() {
    }

    public AuthResult(AuthResultBuilder builder) {
        this.resultCode = builder.getResultCode();
        this.accessToken = builder.getAccessToken();
        this.refreshToken = builder.getRefreshToken();
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public void setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
