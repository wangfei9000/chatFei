package com.chatfei.realtime;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
 private final ChatWebSocketHandler handler;public WebSocketConfig(ChatWebSocketHandler h){handler=h;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/ws").setAllowedOrigins("*");}
}

