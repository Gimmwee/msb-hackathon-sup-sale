package com.msb.supsale.controller;

import com.msb.supsale.dto.FeedbackRequest;
import com.msb.supsale.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) { this.feedbackService = feedbackService; }

    @PostMapping("/feedback")
    public Map<String, Object> feedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.save(request.getSessionId(), request.getRating(), request.getComment());
        return Map.of("status", "success");
    }
}
