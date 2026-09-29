package com.chatfei.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="media_files")
public class MediaFile {
    public enum Type { IMAGE, VIDEO }
    @Id private UUID id;
    @Column(nullable=false) private UUID ownerId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12) private Type mediaType;
    @Column(nullable=false,length=100) private String mimeType;
    @Column(length=255) private String originalName;
    @Column(nullable=false) private long fileSize;
    private Integer width; private Integer height; private Long durationMs;
    @JdbcTypeCode(SqlTypes.VARBINARY) @Basic(fetch=FetchType.LAZY) @Column(columnDefinition="bytea") private byte[] thumbnailData;
    @JdbcTypeCode(SqlTypes.VARBINARY) @Basic(fetch=FetchType.LAZY) @Column(nullable=false,columnDefinition="bytea") private byte[] binaryData;
    @Column(nullable=false) private Instant createdAt;
    protected MediaFile(){}
    public MediaFile(UUID id,UUID ownerId,Type type,String mime,String name,byte[] data,byte[] thumbnail){this.id=id;this.ownerId=ownerId;this.mediaType=type;this.mimeType=mime;this.originalName=name;this.fileSize=data.length;this.binaryData=data;this.thumbnailData=thumbnail;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public UUID getOwnerId(){return ownerId;} public Type getMediaType(){return mediaType;}
    public String getMimeType(){return mimeType;} public String getOriginalName(){return originalName;} public long getFileSize(){return fileSize;}
    public byte[] getThumbnailData(){return thumbnailData;} public byte[] getBinaryData(){return binaryData;}
}
