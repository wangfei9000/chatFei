package com.chatfei.service;
import com.chatfei.domain.ChatUser;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;
import java.util.concurrent.*;
@Service
public class PresenceService{
 private final ConcurrentMap<UUID,WebSocketSession> sessions=new ConcurrentHashMap<>();
 public void online(UUID userId,WebSocketSession session){WebSocketSession old=sessions.put(userId,session);if(old!=null&&old.isOpen())try{old.close();}catch(Exception ignored){}}
 public void offline(UUID userId,WebSocketSession session){sessions.remove(userId,session);}
 public boolean isOnline(UUID id){WebSocketSession s=sessions.get(id);return s!=null&&s.isOpen();}
 public Collection<UUID> onlineIds(){return List.copyOf(sessions.keySet());}
 public Optional<WebSocketSession> session(UUID id){return Optional.ofNullable(sessions.get(id));}
}

