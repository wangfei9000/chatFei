package com.chatfei.repo;
import com.chatfei.domain.ChatMessage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MessageRepository extends JpaRepository<ChatMessage,UUID>{
 Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId,Pageable pageable);
 Optional<ChatMessage> findBySenderIdAndClientMessageId(UUID senderId,String clientMessageId);
 Optional<ChatMessage> findFirstByMediaId(UUID mediaId);
}
