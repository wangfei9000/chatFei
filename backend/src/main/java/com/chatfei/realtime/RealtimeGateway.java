package com.chatfei.realtime;
import com.chatfei.service.PresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import java.util.*;
@Component
public class RealtimeGateway{
 private final PresenceService presence;private final ObjectMapper json;
 public RealtimeGateway(PresenceService p,ObjectMapper j){presence=p;json=j;}
 public void send(UUID userId,String type,Object data){presence.session(userId).ifPresent(s->{try{s.sendMessage(new TextMessage(json.writeValueAsString(Map.of("type",type,"data",data))));}catch(Exception ignored){}});}
 public void send(Collection<UUID> ids,String type,Object data){ids.forEach(id->send(id,type,data));}
 public void broadcast(String type,Object data){send(presence.onlineIds(),type,data);}
}

