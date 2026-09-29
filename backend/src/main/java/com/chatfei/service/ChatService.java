package com.chatfei.service;

import com.chatfei.api.ApiException;
import com.chatfei.domain.*;
import com.chatfei.repo.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ChatService{
 private final ConversationRepository conversations;private final MemberRepository members;private final MessageRepository messages;private final UserRepository users;private final MediaRepository media;
 public ChatService(ConversationRepository c,MemberRepository m,MessageRepository msg,UserRepository u,MediaRepository mediaRepo){conversations=c;members=m;messages=msg;users=u;media=mediaRepo;}
 public record ConversationView(UUID id,String type,String name,List<UserView> members){}
 public record UserView(UUID id,String nickname){}
 public record MessageView(UUID id,String clientMessageId,UUID conversationId,UUID senderId,String senderNickname,String type,String text,UUID mediaId,java.time.Instant createdAt){}

 @Transactional public ConversationView createPrivate(ChatUser me,UUID otherId){
  if(me.getId().equals(otherId))throw new ApiException(HttpStatus.BAD_REQUEST,"不能和自己创建私聊");
  ChatUser other=users.findById(otherId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"用户不存在"));
  for(ConversationMember mine:members.findByUserIdAndLeftAtIsNull(me.getId())){
   Conversation c=conversations.findById(mine.getConversationId()).orElse(null);
   if(c!=null&&c.getType()==Conversation.Type.PRIVATE&&members.existsByConversationIdAndUserIdAndLeftAtIsNull(c.getId(),otherId))return view(c);
  }
  Conversation c=conversations.save(new Conversation(UUID.randomUUID(),Conversation.Type.PRIVATE,null,me.getId()));
  members.save(new ConversationMember(c.getId(),me.getId(),ConversationMember.Role.OWNER));members.save(new ConversationMember(c.getId(),other.getId(),ConversationMember.Role.MEMBER));return view(c);
 }
 @Transactional public ConversationView createGroup(ChatUser me,String name,List<UUID> ids){
  String clean=name==null?"":name.trim();if(clean.length()<2||clean.length()>40)throw new ApiException(HttpStatus.BAD_REQUEST,"群名称需要2到40个字符");
  LinkedHashSet<UUID> all=new LinkedHashSet<>(ids==null?List.of():ids);all.add(me.getId());
  if(all.size()<3)throw new ApiException(HttpStatus.BAD_REQUEST,"群聊至少需要3人");if(all.size()>100)throw new ApiException(HttpStatus.BAD_REQUEST,"群聊最多100人");
  if(users.findAllById(all).size()!=all.size())throw new ApiException(HttpStatus.BAD_REQUEST,"成员不存在");
  Conversation c=conversations.save(new Conversation(UUID.randomUUID(),Conversation.Type.GROUP,clean,me.getId()));
  for(UUID id:all)members.save(new ConversationMember(c.getId(),id,id.equals(me.getId())?ConversationMember.Role.OWNER:ConversationMember.Role.MEMBER));return view(c);
 }
 @Transactional(readOnly=true) public List<ConversationView> list(ChatUser me){return members.findByUserIdAndLeftAtIsNull(me.getId()).stream().map(x->conversations.findById(x.getConversationId()).orElse(null)).filter(Objects::nonNull).sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed()).map(this::view).toList();}
 @Transactional public MessageView send(ChatUser me,UUID conversationId,String clientId,ChatMessage.Type type,String text,UUID mediaId){
  requireMember(conversationId,me.getId());
  Optional<ChatMessage> existing=messages.findBySenderIdAndClientMessageId(me.getId(),clientId);if(existing.isPresent())return messageView(existing.get());
  if(type==ChatMessage.Type.TEXT&&(text==null||text.isBlank()))throw new ApiException(HttpStatus.BAD_REQUEST,"消息不能为空");
  if(type==ChatMessage.Type.IMAGE||type==ChatMessage.Type.VIDEO){MediaFile f=media.findById(mediaId).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"媒体不存在"));if(!f.getOwnerId().equals(me.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"不能发送其他用户的媒体");if(!f.getMediaType().name().equals(type.name()))throw new ApiException(HttpStatus.BAD_REQUEST,"媒体类型不匹配");}
  ChatMessage m=messages.save(new ChatMessage(UUID.randomUUID(),clientId,conversationId,me.getId(),type,text==null?null:text.trim(),mediaId));
  Conversation c=conversations.findById(conversationId).orElseThrow();c.recordMessage(m.getId());return messageView(m);
 }
 @Transactional(readOnly=true) public List<MessageView> history(ChatUser me,UUID id,int limit){requireMember(id,me.getId());return messages.findByConversationIdOrderByCreatedAtDesc(id,PageRequest.of(0,Math.min(Math.max(limit,1),100))).getContent().stream().map(this::messageView).toList();}
 public List<UUID> memberIds(UUID conversationId){return members.findByConversationIdAndLeftAtIsNull(conversationId).stream().map(ConversationMember::getUserId).toList();}
 public void requireMember(UUID conversationId,UUID userId){if(!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId,userId))throw new ApiException(HttpStatus.FORBIDDEN,"无权访问该会话");}
 private ConversationView view(Conversation c){List<UserView> people=members.findByConversationIdAndLeftAtIsNull(c.getId()).stream().map(m->users.findById(m.getUserId()).map(u->new UserView(u.getId(),u.getNickname())).orElse(null)).filter(Objects::nonNull).toList();String name=c.getType()==Conversation.Type.GROUP?c.getName():people.stream().map(UserView::nickname).reduce((a,b)->a+"、"+b).orElse("私聊");return new ConversationView(c.getId(),c.getType().name(),name,people);}
 private MessageView messageView(ChatMessage m){String n=users.findById(m.getSenderId()).map(ChatUser::getNickname).orElse("已注销用户");return new MessageView(m.getId(),m.getClientMessageId(),m.getConversationId(),m.getSenderId(),n,m.getType().name(),m.getTextContent(),m.getMediaId(),m.getCreatedAt());}
}
