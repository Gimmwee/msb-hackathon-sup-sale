package com.msb.supsale.repository;

import com.msb.supsale.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
