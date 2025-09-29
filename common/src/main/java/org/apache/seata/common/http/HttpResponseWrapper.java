package org.apache.seata.common.http;

public class HttpResponseWrapper<T> {
    private int statusCode;
    private String response;

    private T obj;

    public HttpResponseWrapper() {

    }

    public HttpResponseWrapper(int statusCode, String response, T obj) {
        this.statusCode = statusCode;
        this.response = response;
        this.obj = obj;
    }


    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }
}

