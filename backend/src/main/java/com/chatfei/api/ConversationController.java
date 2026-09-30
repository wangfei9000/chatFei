package com.chatfei.api;
import com.chatfei.domain.*;
import com.chatfei.realtime.RealtimeGateway;
import com.chatfei.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/conversations")
public class ConversationController{
 private final AuthService auth;private final ChatService chat;private final RealtimeGateway realtime;
 /** 注入认证、聊天业务和实时事件发送组件。 */
 public ConversationController(AuthService a,ChatService c,RealtimeGateway r){auth=a;chat=c;realtime=r;}
 /** 返回当前登录用户参加的全部会话。 */
 @GetMapping public List<ChatService.ConversationView> list(@RequestHeader(value="Authorization",required=false)String t){return chat.list(auth.require(t));}
 public record PrivateRequest(UUID userId){}
 /** 创建或取得两名用户之间的私聊，并通知双方会话已经创建。 */
 @PostMapping("/private") public ChatService.ConversationView privateChat(@RequestHeader(value="Authorization",required=false)String t,@RequestBody PrivateRequest b){ChatService.ConversationView v=chat.createPrivate(auth.require(t),b.userId());realtime.send(v.members().stream().map(ChatService.UserView::id).toList(),"CONVERSATION_CREATED",v);return v;}
 public record GroupRequest(String name,List<UUID> memberIds){}
 /** 创建群聊、保存成员关系，并向全部群成员推送会话创建事件。 */
 @PostMapping("/groups") public ChatService.ConversationView group(@RequestHeader(value="Authorization",required=false)String t,@RequestBody GroupRequest b){ChatService.ConversationView v=chat.createGroup(auth.require(t),b.name(),b.memberIds());realtime.send(v.members().stream().map(ChatService.UserView::id).toList(),"CONVERSATION_CREATED",v);return v;}
 public record SendRequest(String clientMessageId,String type,String text,String encryptedText,UUID mediaId){}
 /** 校验并保存一条消息，然后通过 WebSocket 通知该会话的在线成员。 */
 @PostMapping("/{id}/messages") public ChatService.MessageView send(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id,@RequestBody SendRequest b){ChatUser me=auth.require(t);ChatMessage.Type type=ChatMessage.Type.valueOf(b.type());ChatService.MessageView v=chat.send(me,id,b.clientMessageId(),type,b.text(),b.encryptedText(),b.mediaId());realtime.send(chat.memberIds(id),"MESSAGE_CREATED",v);return v;}
 /** 分页限制在 1 到 100 条之间，返回指定会话的最近消息记录。 */
 @GetMapping("/{id}/messages") public List<ChatService.MessageView> history(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id,@RequestParam(defaultValue="50")int limit){return chat.history(auth.require(t),id,limit);}
}
