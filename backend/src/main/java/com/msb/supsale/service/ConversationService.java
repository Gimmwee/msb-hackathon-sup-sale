package com.msb.supsale.service;

import com.msb.supsale.model.Conversation;
import com.msb.supsale.model.Message;
import com.msb.supsale.repository.ConversationRepository;
import com.msb.supsale.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void ensureConversation(String sessionId, String platform) {
        if (conversationRepository.findBySessionId(sessionId).isEmpty()) {
            Conversation conv = new Conversation();
            conv.setSessionId(sessionId);
            conv.setPlatform(platform);
            conversationRepository.save(conv);
        }
    }

    @Transactional
    public void saveMessage(String sessionId, String role, String content) {
        Message msg = new Message();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        messageRepository.save(msg);
    }

    public List<Message> getHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
