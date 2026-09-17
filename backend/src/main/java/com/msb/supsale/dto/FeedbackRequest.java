package com.msb.supsale.dto;

import jakarta.validation.constraints.*;

public class FeedbackRequest {
    @NotBlank
    private String sessionId;
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;
    @Size(max = 500)
    private String comment;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
