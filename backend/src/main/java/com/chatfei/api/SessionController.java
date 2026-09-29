package com.chatfei.api;
import com.chatfei.domain.ChatUser;
import com.chatfei.realtime.RealtimeGateway;
import com.chatfei.repo.UserRepository;
import com.chatfei.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class SessionController{
 private final AuthService auth;private final UserRepository users;private final PresenceService presence;private final RealtimeGateway realtime;
 public SessionController(AuthService a,UserRepository u,PresenceService p,RealtimeGateway r){auth=a;users=u;presence=p;realtime=r;}
 @PostMapping("/session") public AuthService.Session create(){return auth.create();}
 public record Me(UUID userId,String nickname,long nicknameVersion){}
 @GetMapping("/users/me") public Me me(@RequestHeader(value="Authorization",required=false)String token){ChatUser u=auth.require(token);return new Me(u.getId(),u.getNickname(),u.getNicknameVersion());}
 public record RenameRequest(String nickname){}
 @PutMapping("/users/me/nickname") public Me rename(@RequestHeader(value="Authorization",required=false)String token,@RequestBody RenameRequest body){ChatUser u=auth.require(token);String n=body.nickname()==null?"":body.nickname().trim();if(n.length()<2||n.length()>16)throw new IllegalArgumentException("昵称需要2到16个字符");u.rename(n);users.save(u);Me result=new Me(u.getId(),u.getNickname(),u.getNicknameVersion());realtime.broadcast("USER_NICKNAME_CHANGED",result);return result;}
 public record OnlineUser(UUID userId,String nickname){}
 @GetMapping("/users/online") public List<OnlineUser> online(@RequestHeader(value="Authorization",required=false)String token){ChatUser me=auth.require(token);return users.findAllById(presence.onlineIds()).stream().filter(u->!u.getId().equals(me.getId())).map(u->new OnlineUser(u.getId(),u.getNickname())).toList();}
}

