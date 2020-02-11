package com.vergilyn.examples.enums;

public enum RspStatusEnum {

    SUCCESS(200,"成功"),

    FAIL(999,"失败"),

    EXCEPTION(500,"系统异常"),

    GATEWAY_DEFAULT_HYSTRIX(100400, "网关默认服务降级"),

    HYSTRIX(100401, "服务降级");

    private int code;

    private String message;

    RspStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
