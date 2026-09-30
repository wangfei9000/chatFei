package com.chatfei.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="chat_messages", uniqueConstraints=@UniqueConstraint(columnNames={"senderId","clientMessageId"}), indexes=@Index(name="idx_message_conversation_time",columnList="conversationId,createdAt"))
public class ChatMessage {
    public enum Type { TEXT, IMAGE, VIDEO, SYSTEM }
    @Id private UUID id;
    @Column(nullable=false,length=80) private String clientMessageId;
    @Column(nullable=false) private UUID conversationId;
    @Column(nullable=false) private UUID senderId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12) private Type type;
    @Column(length=4000) private String textContent;
    @Column(columnDefinition="TEXT") private String encryptedContent;
    @Column(columnDefinition="TEXT") private String plainTextContent;
    private UUID mediaId;
    @Column(nullable=false) private Instant createdAt;
    protected ChatMessage(){}
    public ChatMessage(UUID id,String clientId,UUID conversationId,UUID senderId,Type type,String text,String encryptedText,String plainText,UUID mediaId){this.id=id;this.clientMessageId=clientId;this.conversationId=conversationId;this.senderId=senderId;this.type=type;this.textContent=text;this.encryptedContent=encryptedText;this.plainTextContent=plainText;this.mediaId=mediaId;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public String getClientMessageId(){return clientMessageId;} public UUID getConversationId(){return conversationId;}
    public UUID getSenderId(){return senderId;} public Type getType(){return type;} public String getTextContent(){return textContent;}
    public String getEncryptedContent(){return encryptedContent;} public String getPlainTextContent(){return plainTextContent;}
    public UUID getMediaId(){return mediaId;} public Instant getCreatedAt(){return createdAt;}
}
