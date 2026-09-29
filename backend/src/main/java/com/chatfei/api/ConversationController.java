package com.chatfei.api;
import com.chatfei.domain.*;
import com.chatfei.realtime.RealtimeGateway;
import com.chatfei.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/conversations")
public class ConversationController{
 private final AuthService auth;private final ChatService chat;private final RealtimeGateway realtime;
 public ConversationController(AuthService a,ChatService c,RealtimeGateway r){auth=a;chat=c;realtime=r;}
 @GetMapping public List<ChatService.ConversationView> list(@RequestHeader(value="Authorization",required=false)String t){return chat.list(auth.require(t));}
 public record PrivateRequest(UUID userId){}
 @PostMapping("/private") public ChatService.ConversationView privateChat(@RequestHeader(value="Authorization",required=false)String t,@RequestBody PrivateRequest b){ChatService.ConversationView v=chat.createPrivate(auth.require(t),b.userId());realtime.send(v.members().stream().map(ChatService.UserView::id).toList(),"CONVERSATION_CREATED",v);return v;}
 public record GroupRequest(String name,List<UUID> memberIds){}
 @PostMapping("/groups") public ChatService.ConversationView group(@RequestHeader(value="Authorization",required=false)String t,@RequestBody GroupRequest b){ChatService.ConversationView v=chat.createGroup(auth.require(t),b.name(),b.memberIds());realtime.send(v.members().stream().map(ChatService.UserView::id).toList(),"CONVERSATION_CREATED",v);return v;}
 public record SendRequest(String clientMessageId,String type,String text,UUID mediaId){}
 @PostMapping("/{id}/messages") public ChatService.MessageView send(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id,@RequestBody SendRequest b){ChatUser me=auth.require(t);ChatMessage.Type type=ChatMessage.Type.valueOf(b.type());ChatService.MessageView v=chat.send(me,id,b.clientMessageId(),type,b.text(),b.mediaId());realtime.send(chat.memberIds(id),"MESSAGE_CREATED",v);return v;}
 @GetMapping("/{id}/messages") public List<ChatService.MessageView> history(@RequestHeader(value="Authorization",required=false)String t,@PathVariable UUID id,@RequestParam(defaultValue="50")int limit){return chat.history(auth.require(t),id,limit);}
}

