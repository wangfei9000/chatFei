package com.chatfei.api;
import com.chatfei.domain.*;
import com.chatfei.repo.*;
import com.chatfei.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
@RestController @RequestMapping("/api/media")
public class MediaController{
 private final AuthService auth;private final MediaRepository media;private final MessageRepository messages;private final MemberRepository members;
 @Value("${chatfei.media.max-image-bytes}") long maxImage;@Value("${chatfei.media.max-video-bytes}") long maxVideo;
 public MediaController(AuthService a,MediaRepository m,MessageRepository msg,MemberRepository memberRepo){auth=a;media=m;messages=msg;members=memberRepo;}
 public record Uploaded(UUID mediaId,String type,String mimeType,long fileSize){}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Uploaded upload(@RequestHeader(value="Authorization",required=false)String t,@RequestPart("file")MultipartFile file,@RequestPart(value="thumbnail",required=false)MultipartFile thumbnail)throws IOException{
  ChatUser me=auth.require(t);String mime=Optional.ofNullable(file.getContentType()).orElse("");MediaFile.Type type=mime.startsWith("image/")?MediaFile.Type.IMAGE:mime.startsWith("video/")?MediaFile.Type.VIDEO:null;if(type==null)throw new IllegalArgumentException("只允许图片或视频");long max=type==MediaFile.Type.IMAGE?maxImage:maxVideo;if(file.getSize()>max)throw new IllegalArgumentException("媒体文件过大");byte[] thumb=thumbnail==null?null:thumbnail.getBytes();MediaFile saved=media.save(new MediaFile(UUID.randomUUID(),me.getId(),type,mime,file.getOriginalFilename(),file.getBytes(),thumb));return new Uploaded(saved.getId(),type.name(),mime,saved.getFileSize());
 }
 @GetMapping("/{id}/content") public ResponseEntity<byte[]> content(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id){ChatUser me=auth.require(t);MediaFile f=requireAccessible(me,id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getMimeType())).contentLength(f.getFileSize()).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePrivate()).body(f.getBinaryData());}
 @GetMapping("/{id}/thumbnail") public ResponseEntity<byte[]> thumbnail(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id){ChatUser me=auth.require(t);MediaFile f=requireAccessible(me,id);byte[] bytes=f.getThumbnailData()==null?f.getBinaryData():f.getThumbnailData();return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getMimeType())).body(bytes);}
 private MediaFile requireAccessible(ChatUser me,UUID id){MediaFile f=media.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"媒体不存在"));if(f.getOwnerId().equals(me.getId()))return f;boolean allowed=messages.findFirstByMediaId(id).map(m->members.existsByConversationIdAndUserIdAndLeftAtIsNull(m.getConversationId(),me.getId())).orElse(false);if(!allowed)throw new ApiException(HttpStatus.FORBIDDEN,"无权访问该媒体");return f;}
}
