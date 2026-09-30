package com.chatfei.service;

import com.chatfei.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesTextCrypto {
    private static final int IV_SIZE = 12;
    private final SecretKeySpec key;
    public AesTextCrypto(@Value("${chatfei.crypto.aes-key-base64}") String keyBase64) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(keyBase64.trim()); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("chatfei.crypto.aes-key-base64 不是有效的 Base64", e); }
        if (bytes.length != 32) throw new IllegalStateException("chatfei.crypto.aes-key-base64 必须是 32 字节 AES-256 密钥");
        key = new SecretKeySpec(bytes, "AES");
    }
    public String decrypt(String encryptedText) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            if (payload.length <= IV_SIZE) throw new IllegalArgumentException("payload too short");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(payload, 0, IV_SIZE)));
            return new String(cipher.doFinal(Arrays.copyOfRange(payload, IV_SIZE, payload.length)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "消息解密失败，请检查客户端 AES 密钥");
        }
    }
}
