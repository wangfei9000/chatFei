package com.chatfei.realtime;
import com.chatfei.service.PresenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import java.util.*;
@Component
public class RealtimeGateway{
 private final PresenceService presence;private final ObjectMapper json;
 /** 注入在线连接管理器和 JSON 序列化器。 */
 public RealtimeGateway(PresenceService p,ObjectMapper j){presence=p;json=j;}
 /** 向指定在线用户发送实时事件；用户离线或连接不存在时不执行发送。 */
 public void send(UUID userId,String type,Object data){presence.session(userId).ifPresent(s->{try{s.sendMessage(new TextMessage(json.writeValueAsString(Map.of("type",type,"data",data))));}catch(Exception ignored){}});}
 /** 逐个向一组用户发送同一个实时事件，离线用户会被自动跳过。 */
 public void send(Collection<UUID> ids,String type,Object data){ids.forEach(id->send(id,type,data));}
 /** 向当前所有在线用户广播事件，例如用户上线或离线状态变化。 */
 public void broadcast(String type,Object data){send(presence.onlineIds(),type,data);}
}
