package com.chatfei.repo;
import com.chatfei.domain.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<ChatUser,UUID>{Optional<ChatUser> findByTokenHash(String tokenHash);}

