package com.chatfei.repo;
import com.chatfei.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemberRepository extends JpaRepository<ConversationMember,UUID>{
 List<ConversationMember> findByUserIdAndLeftAtIsNull(UUID userId);
 List<ConversationMember> findByConversationIdAndLeftAtIsNull(UUID conversationId);
 boolean existsByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId,UUID userId);
 Optional<ConversationMember> findByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId,UUID userId);
}

