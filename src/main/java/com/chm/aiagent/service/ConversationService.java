package com.chm.aiagent.service;

import com.chm.aiagent.dto.ConversationVO;
import com.chm.aiagent.model.Conversation;
import com.chm.aiagent.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationVO create(String agentId, String userId, String title) {
        // 清理该用户在此 Agent 下所有未发过消息的历史空会话
        conversationRepository.deleteEmptyByUserAndAgent(userId, agentId);

        Conversation c = new Conversation();
        c.setId(UUID.randomUUID().toString());
        c.setAgentId(agentId);
        c.setUserId(userId);
        c.setTitle(title != null ? title : "新对话");
        c.setMessageCount(0);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        conversationRepository.insert(c);
        return toVO(c);
    }

    public List<ConversationVO> list(String userId, String agentId, int page, int size) {
        int offset = (page - 1) * size;
        return conversationRepository.findByUser(userId, agentId, offset, size)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public int count(String userId, String agentId) {
        return conversationRepository.countByUser(userId, agentId);
    }

    public ConversationVO getById(String id) {
        return toVO(conversationRepository.findById(id));
    }

    public void delete(String id) {
        conversationRepository.deleteById(id);
    }

    public void rename(String id, String title) {
        conversationRepository.rename(id, title);
    }

    public void touchAfterMessage(String id, String lastMessage) {
        conversationRepository.updateAfterMessage(id, lastMessage, LocalDateTime.now());
    }

    public Conversation getEntity(String id) {
        return conversationRepository.findById(id);
    }

    private ConversationVO toVO(Conversation c) {
        if (c == null) return null;
        ConversationVO vo = new ConversationVO();
        vo.setId(c.getId());
        vo.setAgentId(c.getAgentId());
        vo.setTitle(c.getTitle());
        vo.setLastMessage(c.getLastMessage());
        vo.setMessageCount(c.getMessageCount());
        vo.setCreatedAt(c.getCreatedAt());
        vo.setUpdatedAt(c.getUpdatedAt());
        return vo;
    }
}
