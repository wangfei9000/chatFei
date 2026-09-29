package com.chatfei.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {
    public enum Type { PRIVATE, GROUP }
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Type type;
    @Column(length = 40) private String name;
    private UUID ownerId;
    private UUID lastMessageId;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Conversation() {}
    public Conversation(UUID id, Type type, String name, UUID ownerId) {
        this.id=id; this.type=type; this.name=name; this.ownerId=ownerId;
        this.createdAt=Instant.now(); this.updatedAt=createdAt;
    }
    public UUID getId(){return id;} public Type getType(){return type;} public String getName(){return name;}
    public UUID getOwnerId(){return ownerId;} public UUID getLastMessageId(){return lastMessageId;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void recordMessage(UUID messageId){lastMessageId=messageId;updatedAt=Instant.now();}
}

