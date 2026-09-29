package com.chatfei.repo;
import com.chatfei.domain.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface MediaRepository extends JpaRepository<MediaFile,UUID>{}

