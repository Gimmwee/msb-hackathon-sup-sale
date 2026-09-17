package com.msb.supsale.controller;

import com.msb.supsale.dto.ConversationDto;
import com.msb.supsale.model.Conversation;
import com.msb.supsale.model.Message;
import com.msb.supsale.repository.ConversationRepository;
import com.msb.supsale.repository.MessageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/conversations")
public class AdminConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public AdminConversationController(ConversationRepository conversationRepository,
                                       MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public List<ConversationDto> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream().map(conv -> {
            ConversationDto dto = new ConversationDto();
            dto.setId(conv.getId());
            dto.setSessionId(conv.getSessionId());
            dto.setPlatform(conv.getPlatform());
            dto.setCreatedAt(conv.getCreatedAt());
            List<Message> msgs = messageRepository.findBySessionIdOrderByCreatedAtAsc(conv.getSessionId());
            dto.setMessages(msgs.stream().map(ConversationDto.MessageDto::new).toList());
            return dto;
        }).toList();
    }

    @GetMapping("/{sessionId}")
    public ConversationDto getConversation(@PathVariable String sessionId) {
        Conversation conv = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());
        dto.setSessionId(conv.getSessionId());
        dto.setPlatform(conv.getPlatform());
        dto.setCreatedAt(conv.getCreatedAt());
        List<Message> msgs = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        dto.setMessages(msgs.stream().map(ConversationDto.MessageDto::new).toList());
        return dto;
    }
}
