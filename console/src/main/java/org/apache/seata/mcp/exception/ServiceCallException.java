package org.apache.seata.mcp.exception;

import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ServiceCallException extends RuntimeException {
    private final HttpStatusCode httpStatus;
    private final Instant timestamp;

    public ServiceCallException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
        this.timestamp = Instant.now();
    }

    public ServiceCallException(String message) {
        super(message);
        this.httpStatus = null;
        this.timestamp = Instant.now();
    }

    public ServiceCallException(String message, HttpStatusCode httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
    }

    public Map<String, Object> toErrorResponse() {
        Map<String, Object> error = new HashMap<>();
        error.put("message", this.getMessage());
        error.put("timestamp", timestamp.toString());
        error.put("httpStatus", httpStatus != null ? httpStatus.value() : null);
        return error;
    }
}
