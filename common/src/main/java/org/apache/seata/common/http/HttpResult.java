package org.apache.seata.common.http;

public class HttpResult<T> {

    private int statusCode;
    private String body;
    private T rawResponse;

    public HttpResult() {}

    public HttpResult(int statusCode, String body, T rawResponse) {
        this.statusCode = statusCode;
        this.body = body;
        this.rawResponse = rawResponse;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public T getRawResponse() { return rawResponse; }
    public void setRawResponse(T rawResponse) { this.rawResponse = rawResponse; }
}
