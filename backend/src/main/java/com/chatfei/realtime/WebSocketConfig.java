package com.chatfei.realtime;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
 /** 保存负责处理连接生命周期和心跳消息的 WebSocket 处理器。 */
 private final ChatWebSocketHandler handler;public WebSocketConfig(ChatWebSocketHandler h){handler=h;}
 /** 将处理器注册到 /ws 地址，使手机客户端可以建立 WebSocket 长连接。 */
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/ws").setAllowedOrigins("*");}
}
