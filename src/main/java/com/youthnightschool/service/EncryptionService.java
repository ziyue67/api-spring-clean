package com.youthnightschool.service;

import com.youthnightschool.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Encrypts and decrypts WeChat session keys.
 * Mirrors the NestJS SessionsService encryption logic exactly:
 * - SHA-256 hash of env var SESSION_KEY_ENCRYPTION_KEY used as AES-256 key
 * - New format "v2:{iv_hex}:{tag_hex}:{encrypted_hex}" uses AES-256-GCM (12-byte IV, 128-bit tag)
 * - Legacy format "{iv_hex}:{encrypted_hex}" uses AES-256-CBC (16-byte IV)
 */
@Service
public class EncryptionService {

  private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
  private static final String CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int CBC_IV_LENGTH = 16;
  private static final String V2_PREFIX = "v2:";

  private final SecretKeySpec aesKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public EncryptionService(AppProperties appProperties) {
    String secret = appProperties.getEncryption().getSessionKey();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "app.encryption.session-key must be configured");
    }
    this.aesKey = deriveKey(secret);
  }

  /**
   * Derives a 256-bit AES key by SHA-256 hashing the secret string.
   */
  private SecretKeySpec deriveKey(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new RuntimeException("Failed to derive encryption key", e);
    }
  }

  /**
   * Encrypts plaintext using AES-256-GCM.
   * Output format: "v2:{iv_hex}:{tag_hex}:{encrypted_hex}"
   */
  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);

      byte[] cipherOutput = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Java GCM appends the 16-byte tag to the ciphertext
      int tagLength = GCM_TAG_LENGTH_BITS / 8; // 16 bytes
      int ciphertextLength = cipherOutput.length - tagLength;

      byte[] ciphertext = new byte[ciphertextLength];
      byte[] tag = new byte[tagLength];
      System.arraycopy(cipherOutput, 0, ciphertext, 0, ciphertextLength);
      System.arraycopy(cipherOutput, ciphertextLength, tag, 0, tagLength);

      HexFormat hex = HexFormat.of();
      return V2_PREFIX
          + hex.formatHex(iv) + ":"
          + hex.formatHex(tag) + ":"
          + hex.formatHex(ciphertext);
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypts a payload string.
   * If it starts with "v2:" uses GCM decryption.
   * Otherwise uses legacy CBC decryption (16-byte IV, AES/CBC/PKCS5Padding).
   */
  public String decrypt(String payload) {
    if (payload.startsWith(V2_PREFIX)) {
      return decryptGcm(payload);
    }
    return decryptCbc(payload);
  }

  private String decryptGcm(String payload) {
    // Format: v2:{iv_hex}:{tag_hex}:{encrypted_hex}
    String[] parts = payload.split(":");
    if (parts.length != 4 || !parts[0].equals("v2")) {
      throw new RuntimeException("Invalid encrypted session payload");
    }

    String ivHex = parts[1];
    String tagHex = parts[2];
    String encryptedHex = parts[3];

    if (ivHex.isEmpty() || tagHex.isEmpty() || encryptedHex.isEmpty()) {
      throw new RuntimeException("Invalid encrypted session payload");
    }

    try {
      HexFormat hex = HexFormat.of();
      byte[] iv = hex.parseHex(ivHex);
      byte[] tag = hex.parseHex(tagHex);
      byte[] ciphertext = hex.parseHex(encryptedHex);

      // Reconstruct the Java GCM format: ciphertext || tag
      byte[] cipherOutput = new byte[ciphertext.length + tag.length];
      System.arraycopy(ciphertext, 0, cipherOutput, 0, ciphertext.length);
      System.arraycopy(tag, 0, cipherOutput, ciphertext.length, tag.length);

      Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

      byte[] plainBytes = cipher.doFinal(cipherOutput);
      return new String(plainBytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("GCM decryption failed", e);
    }
  }

  private String decryptCbc(String payload) {
    String[] parts = payload.split(":");
    if (parts.length != 2) {
      return payload;
    }

    String ivHex = parts[0];
    String encryptedHex = parts[1];

    if (ivHex.isEmpty() || encryptedHex.isEmpty()) {
      return payload;
    }

    try {
      HexFormat hex = HexFormat.of();
      byte[] iv = hex.parseHex(ivHex);
      byte[] ciphertext = hex.parseHex(encryptedHex);

      Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);

      byte[] plainBytes = cipher.doFinal(ciphertext);
      return new String(plainBytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("CBC decryption failed", e);
    }
  }
}
