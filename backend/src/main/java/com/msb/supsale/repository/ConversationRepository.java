package com.msb.supsale.repository;

import com.msb.supsale.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findBySessionId(String sessionId);
}
