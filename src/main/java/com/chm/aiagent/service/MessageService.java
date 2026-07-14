package com.chm.aiagent.service;

import com.chm.aiagent.dto.MessageVO;
import com.chm.aiagent.model.Message;
import com.chm.aiagent.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public void save(String conversationId, String userId, String role, String content) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        messageRepository.insert(m);
    }

    public List<MessageVO> list(String conversationId, int page, int size) {
        int offset = (page - 1) * size;
        return messageRepository.findByConversation(conversationId, offset, size)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public int count(String conversationId) {
        return messageRepository.countByConversation(conversationId);
    }

    public void updateFeedback(Long msgId, String feedback) {
        messageRepository.updateFeedback(msgId, feedback);
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setFeedback(m.getFeedback());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
