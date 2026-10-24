package io.github.benxincaomu.oa.base.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeycloakSecretCipher {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String configuredKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeycloakSecretCipher(@Value("${app.keycloak.config-encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plainText) {
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(nonce.length + encrypted.length);
            payload.put(nonce);
            payload.put(encrypted);
            return "v1:" + Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt Keycloak credentials", exception);
        }
    }

    public String decrypt(String cipherText) {
        try {
            if (cipherText == null || !cipherText.startsWith("v1:")) {
                throw new IllegalArgumentException("Unsupported encrypted Keycloak credential format");
            }
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(3));
            if (payload.length <= NONCE_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted Keycloak credential");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_BYTES];
            buffer.get(nonce);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt Keycloak credentials", exception);
        }
    }

    private SecretKeySpec key() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("KEYCLOAK_CONFIG_ENCRYPTION_KEY must be configured before Keycloak setup");
        }
        try {
            byte[] key = Base64.getDecoder().decode(configuredKey);
            if (key.length != 32) {
                throw new IllegalStateException("KEYCLOAK_CONFIG_ENCRYPTION_KEY must decode to exactly 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("KEYCLOAK_CONFIG_ENCRYPTION_KEY must be a Base64 encoded 32-byte key", exception);
        }
    }
}
