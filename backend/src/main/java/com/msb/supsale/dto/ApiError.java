package com.msb.supsale.dto;

import java.time.Instant;

public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String requestId;

    public ApiError(int status, String error, String message, String requestId) {
        this.timestamp = Instant.now(); this.status = status; this.error = error;
        this.message = message; this.requestId = requestId;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getRequestId() { return requestId; }
}
