package com.msb.supsale.service;

import com.msb.supsale.model.Feedback;
import com.msb.supsale.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback save(String sessionId, int rating, String comment) {
        Feedback fb = new Feedback();
        fb.setSessionId(sessionId);
        fb.setRating(rating);
        fb.setComment(comment);
        return feedbackRepository.save(fb);
    }
}
