package com.chatfei.service;

import com.chatfei.api.ApiException;
import com.chatfei.domain.ChatUser;
import com.chatfei.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service
public class AuthService{
 private final UserRepository users; private final SecureRandom random=new SecureRandom();
 public AuthService(UserRepository users){this.users=users;}
 public record Session(UUID userId,String nickname,String token){}
 @Transactional public Session create(){
  byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  UUID id=UUID.randomUUID();String nickname="游客"+(1000+random.nextInt(9000));
  users.save(new ChatUser(id,nickname,hash(token)));return new Session(id,nickname,token);
 }
 @Transactional(readOnly=true) public ChatUser require(String authorization){
  if(authorization==null||!authorization.startsWith("Bearer "))throw new ApiException(HttpStatus.UNAUTHORIZED,"缺少身份令牌");
  return requireToken(authorization.substring(7));
 }
 @Transactional(readOnly=true) public ChatUser requireToken(String token){return users.findByTokenHash(hash(token)).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"身份令牌无效"));}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}

