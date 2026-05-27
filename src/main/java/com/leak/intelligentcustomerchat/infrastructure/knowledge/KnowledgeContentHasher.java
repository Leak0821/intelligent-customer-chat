package com.leak.intelligentcustomerchat.infrastructure.knowledge;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class KnowledgeContentHasher {

    public String hashText(String text) {
        String normalized = normalize(text);
        return sha256Hex(normalized);
    }

    public String shortHash(String text) {
        String hash = hashText(text);
        return hash.substring(0, 12);
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte current : hashed) {
                builder.append(Character.forDigit((current >> 4) & 0xF, 16));
                builder.append(Character.forDigit(current & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
