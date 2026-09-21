package com.campus.community.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private final byte[] secret;
    private final long expirationMinutes;
    private final ObjectMapper objectMapper;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret 至少需要 32 个字符");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
        this.objectMapper = objectMapper;
    }

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", principal.id().toString());
        payload.put("username", principal.username());
        payload.put("role", principal.role());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(expirationMinutes, ChronoUnit.MINUTES).getEpochSecond());
        try {
            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            String content = encodedHeader + "." + encodedPayload;
            return content + "." + encode(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成登录凭证", exception);
        }
    }

    public UserPrincipal parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("登录凭证格式错误");
            }
            String content = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(content), BASE64_URL_DECODER.decode(parts[2]))) {
                throw new IllegalArgumentException("登录凭证签名无效");
            }
            Map<String, Object> claims = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() { });
            long expiresAt = ((Number) claims.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new IllegalArgumentException("登录凭证已过期");
            }
            return new UserPrincipal(
                    Long.parseLong(String.valueOf(claims.get("sub"))),
                    String.valueOf(claims.get("username")),
                    String.valueOf(claims.get("role"))
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("登录凭证无效", exception);
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }
}
