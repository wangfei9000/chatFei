package com.chatfei.service;
import com.chatfei.domain.ChatUser;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;
import java.util.concurrent.*;
@Service
public class PresenceService{
 private final ConcurrentMap<UUID,WebSocketSession> sessions=new ConcurrentHashMap<>();
 /** 登记用户的新 WebSocket 连接；同一用户再次连接时关闭并替换旧连接。 */
 public void online(UUID userId,WebSocketSession session){WebSocketSession old=sessions.put(userId,session);if(old!=null&&old.isOpen())try{old.close();}catch(Exception ignored){}}
 /** 在连接关闭后删除用户与该 WebSocket 会话的映射。 */
 public void offline(UUID userId,WebSocketSession session){sessions.remove(userId,session);}
 /** 判断指定用户是否存在并保持着一个打开的 WebSocket 连接。 */
 public boolean isOnline(UUID id){WebSocketSession s=sessions.get(id);return s!=null&&s.isOpen();}
 /** 返回当前登记在线的全部用户 ID 快照，供状态广播使用。 */
 public Collection<UUID> onlineIds(){return List.copyOf(sessions.keySet());}
 /** 查询指定用户的 WebSocket 会话；用户离线时返回空 Optional。 */
 public Optional<WebSocketSession> session(UUID id){return Optional.ofNullable(sessions.get(id));}
}
