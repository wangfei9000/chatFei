package com.chatfei.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_users", indexes = @Index(name = "idx_user_token", columnList = "tokenHash", unique = true))
public class ChatUser {
    @Id private UUID id;
    @Column(nullable = false, length = 16) private String nickname;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private long nicknameVersion;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant lastActiveAt;

    protected ChatUser() {}
    public ChatUser(UUID id, String nickname, String tokenHash) {
        this.id = id; this.nickname = nickname; this.tokenHash = tokenHash;
        this.nicknameVersion = 1; this.createdAt = Instant.now(); this.lastActiveAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getNickname() { return nickname; }
    public String getTokenHash() { return tokenHash; }
    public long getNicknameVersion() { return nicknameVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActiveAt() { return lastActiveAt; }
    public void rename(String value) { nickname = value; nicknameVersion++; }
    public void touch() { lastActiveAt = Instant.now(); }
}

