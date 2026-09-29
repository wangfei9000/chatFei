package com.chatfei.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="conversation_members", uniqueConstraints=@UniqueConstraint(columnNames={"conversationId","userId"}))
public class ConversationMember {
    public enum Role { OWNER, MEMBER }
    @Id private UUID id;
    @Column(nullable=false) private UUID conversationId;
    @Column(nullable=false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12) private Role role;
    private UUID lastReadMessageId;
    @Column(nullable=false) private Instant joinedAt;
    private Instant leftAt;
    protected ConversationMember(){}
    public ConversationMember(UUID conversationId,UUID userId,Role role){this.id=UUID.randomUUID();this.conversationId=conversationId;this.userId=userId;this.role=role;this.joinedAt=Instant.now();}
    public UUID getId(){return id;} public UUID getConversationId(){return conversationId;} public UUID getUserId(){return userId;}
    public Role getRole(){return role;} public UUID getLastReadMessageId(){return lastReadMessageId;} public Instant getLeftAt(){return leftAt;}
    public void markRead(UUID id){lastReadMessageId=id;}
}

