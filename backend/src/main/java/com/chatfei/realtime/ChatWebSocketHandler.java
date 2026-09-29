package com.chatfei.realtime;
import com.chatfei.domain.ChatUser;
import com.chatfei.service.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler{
 private final AuthService auth;private final PresenceService presence;private final RealtimeGateway gateway;private final ObjectMapper json;
 public ChatWebSocketHandler(AuthService a,PresenceService p,RealtimeGateway g,ObjectMapper j){auth=a;presence=p;gateway=g;json=j;}
 @Override public void afterConnectionEstablished(WebSocketSession session)throws Exception{
  String token=UriComponentsBuilder.fromUri(Objects.requireNonNull(session.getUri())).build().getQueryParams().getFirst("token");
  try{ChatUser user=auth.requireToken(token==null?"":token);session.getAttributes().put("userId",user.getId());presence.online(user.getId(),session);gateway.broadcast("USER_ONLINE",Map.of("userId",user.getId(),"nickname",user.getNickname()));}
  catch(Exception e){session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));}
 }
 @Override protected void handleTextMessage(WebSocketSession session,TextMessage message)throws Exception{JsonNode node=json.readTree(message.getPayload());if("PING".equals(node.path("type").asText()))session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));}
 @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){UUID id=(UUID)session.getAttributes().get("userId");if(id!=null){presence.offline(id,session);gateway.broadcast("USER_OFFLINE",Map.of("userId",id));}}
}

