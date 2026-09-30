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
 private final ConversationRepository conversations;private final MemberRepository members;private final MessageRepository messages;private final UserRepository users;private final MediaRepository media;private final AesTextCrypto crypto;
 /** 注入会话、成员、消息、用户、媒体仓库和文本解密组件。 */
 public ChatService(ConversationRepository c,MemberRepository m,MessageRepository msg,UserRepository u,MediaRepository mediaRepo,AesTextCrypto aesTextCrypto){conversations=c;members=m;messages=msg;users=u;media=mediaRepo;crypto=aesTextCrypto;}
 public record ConversationView(UUID id,String type,String name,List<UserView> members){}
 public record UserView(UUID id,String nickname){}
 public record MessageView(UUID id,String clientMessageId,UUID conversationId,UUID senderId,String senderNickname,String type,String text,String encryptedText,UUID mediaId,java.time.Instant createdAt){}

 /** 创建私聊；若双方已有私聊则直接返回原会话，避免重复创建。 */
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
 /** 校验群名称和成员数量，创建群会话并保存全部成员关系。 */
 @Transactional public ConversationView createGroup(ChatUser me,String name,List<UUID> ids){
  String clean=name==null?"":name.trim();if(clean.length()<2||clean.length()>40)throw new ApiException(HttpStatus.BAD_REQUEST,"群名称需要2到40个字符");
  LinkedHashSet<UUID> all=new LinkedHashSet<>(ids==null?List.of():ids);all.add(me.getId());
  if(all.size()<3)throw new ApiException(HttpStatus.BAD_REQUEST,"群聊至少需要3人");if(all.size()>100)throw new ApiException(HttpStatus.BAD_REQUEST,"群聊最多100人");
  if(users.findAllById(all).size()!=all.size())throw new ApiException(HttpStatus.BAD_REQUEST,"成员不存在");
  Conversation c=conversations.save(new Conversation(UUID.randomUUID(),Conversation.Type.GROUP,clean,me.getId()));
  for(UUID id:all)members.save(new ConversationMember(c.getId(),id,id.equals(me.getId())?ConversationMember.Role.OWNER:ConversationMember.Role.MEMBER));return view(c);
 }
 /** 查询当前用户加入且尚未退出的会话，并按最近更新时间倒序返回。 */
 @Transactional(readOnly=true) public List<ConversationView> list(ChatUser me){return members.findByUserIdAndLeftAtIsNull(me.getId()).stream().map(x->conversations.findById(x.getConversationId()).orElse(null)).filter(Objects::nonNull).sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed()).map(this::view).toList();}
 /** 校验发送权限和消息内容，将消息持久化，并更新会话的最后消息时间。 */
 @Transactional public MessageView send(ChatUser me,UUID conversationId,String clientId,ChatMessage.Type type,String text,String encryptedText,UUID mediaId){
  requireMember(conversationId,me.getId());
  Optional<ChatMessage> existing=messages.findBySenderIdAndClientMessageId(me.getId(),clientId);if(existing.isPresent())return messageView(existing.get());
  String plainText=null;if(type==ChatMessage.Type.TEXT){if(encryptedText!=null&&!encryptedText.isBlank())plainText=crypto.decrypt(encryptedText);else if(text==null||text.isBlank())throw new ApiException(HttpStatus.BAD_REQUEST,"消息不能为空");String content=plainText!=null?plainText:text;if(content.length()>4000)throw new ApiException(HttpStatus.BAD_REQUEST,"消息不能超过4000个字符");}
  if(type==ChatMessage.Type.IMAGE||type==ChatMessage.Type.VIDEO){MediaFile f=media.findById(mediaId).orElseThrow(()->new ApiException(HttpStatus.BAD_REQUEST,"媒体不存在"));if(!f.getOwnerId().equals(me.getId()))throw new ApiException(HttpStatus.FORBIDDEN,"不能发送其他用户的媒体");if(!f.getMediaType().name().equals(type.name()))throw new ApiException(HttpStatus.BAD_REQUEST,"媒体类型不匹配");}
  ChatMessage m=messages.save(new ChatMessage(UUID.randomUUID(),clientId,conversationId,me.getId(),type,encryptedText==null&&text!=null?text.trim():null,encryptedText,plainText,mediaId));
  Conversation c=conversations.findById(conversationId).orElseThrow();c.recordMessage(m.getId());return messageView(m);
 }
 /** 校验会话访问权限并查询最近消息，最大返回 100 条。 */
 @Transactional(readOnly=true) public List<MessageView> history(ChatUser me,UUID id,int limit){requireMember(id,me.getId());return messages.findByConversationIdOrderByCreatedAtDesc(id,PageRequest.of(0,Math.min(Math.max(limit,1),100))).getContent().stream().map(this::messageView).toList();}
 /** 返回会话内所有尚未退出的成员 ID，供实时网关确定推送目标。 */
 public List<UUID> memberIds(UUID conversationId){return members.findByConversationIdAndLeftAtIsNull(conversationId).stream().map(ConversationMember::getUserId).toList();}
 /** 确认用户是当前会话成员，否则抛出无权访问异常。 */
 public void requireMember(UUID conversationId,UUID userId){if(!members.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId,userId))throw new ApiException(HttpStatus.FORBIDDEN,"无权访问该会话");}
 /** 将会话实体转换为包含成员昵称的接口响应对象。 */
 private ConversationView view(Conversation c){List<UserView> people=members.findByConversationIdAndLeftAtIsNull(c.getId()).stream().map(m->users.findById(m.getUserId()).map(u->new UserView(u.getId(),u.getNickname())).orElse(null)).filter(Objects::nonNull).toList();String name=c.getType()==Conversation.Type.GROUP?c.getName():people.stream().map(UserView::nickname).reduce((a,b)->a+"、"+b).orElse("私聊");return new ConversationView(c.getId(),c.getType().name(),name,people);}
 /** 将消息实体转换为带发送人昵称和消息内容的接口响应对象。 */
 private MessageView messageView(ChatMessage m){String n=users.findById(m.getSenderId()).map(ChatUser::getNickname).orElse("已注销用户");return new MessageView(m.getId(),m.getClientMessageId(),m.getConversationId(),m.getSenderId(),n,m.getType().name(),m.getTextContent(),m.getEncryptedContent(),m.getMediaId(),m.getCreatedAt());}
}
