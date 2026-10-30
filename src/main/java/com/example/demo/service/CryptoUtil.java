package com.example.demo.service;


import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Mirrors ALL crypto logic from sendtoairpay.jsp and responsefromairpay.jsp:
 *  - AES-256-CBC encrypt (for token request payload)
 *  - AES-256-CBC encrypt with MD5 key (aes256encrypt)
 *  - AES-256-CBC decrypt
 *  - SHA-256 private key generation (encrypt_sha)
 *  - MD5 secret key generation
 *  - Checksum calculation (sorted-values + date → SHA-256)
 *  - CRC32 secure hash for response verification
 */
@Component
public class CryptoUtil {

    // -----------------------------------------------------------------------
    // 1.  MD5 secret key  (Username ~:~ Password → MD5 → hex)
    //     Used as the AES key for encrypting/decrypting API payloads.
    // -----------------------------------------------------------------------
    public String buildSecretKey(String username, String password) throws NoSuchAlgorithmException {
        String combined = username + "~:~" + password;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(combined.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(md.digest());
    }

    // -----------------------------------------------------------------------
    // 2.  AES-256-CBC encrypt  (random 8-byte IV → hex string → used as IV)
    //     Matches the encrypt() method in sendtoairpay.jsp.
    //     Returns:  ivHex (16 chars) + Base64(ciphertext)
    // -----------------------------------------------------------------------
    public String encrypt(String plainText, String secretKey) throws Exception {
        // Random 8-byte IV → 16-char hex string
        byte[] ivBytes = new byte[8];
        new SecureRandom().nextBytes(ivBytes);
        String ivHex = bytesToHex(ivBytes);                         // 16 chars

        // The JSP uses the hex string bytes as the actual IV fed to AES
        IvParameterSpec ivSpec = new IvParameterSpec(ivHex.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec  = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return ivHex + Base64.getEncoder().encodeToString(encrypted);
    }

    // -----------------------------------------------------------------------
    // 3.  AES-256-CBC encrypt using MD5(username~:~password) as key
    //     Matches the static aes256encrypt() in sendtoairpay.jsp.
    //     Returns:  hexIV (32 chars) + Base64(ciphertext)
    // -----------------------------------------------------------------------
//    public String aes256Encrypt(String data, String username, String password) throws Exception {
//        MessageDigest md = MessageDigest.getInstance("MD5");
//        byte[] key = md.digest((username + "~:~" + password).getBytes(StandardCharsets.UTF_8));
//        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
//
//        byte[] iv = new byte[16];
//        new SecureRandom().nextBytes(iv);
//        IvParameterSpec ivSpec = new IvParameterSpec(iv);
//
//        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
//
//        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
//        return bytesToHex(iv) + Base64.getEncoder().encodeToString(encrypted);
//    }
    
    
 // REPLACE the entire aes256Encrypt method with this:
    public String aes256Encrypt(String data, String username, String password) throws Exception {
        // Key: MD5 raw bytes (16 bytes = 128-bit, but AES-256 needs 32 — PHP pads it)
        // Actually PHP md5() returns 32-char hex string, then used as UTF-8 bytes for key!
        String key = buildSecretKey(username, password); // 32-char hex string
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

        // IV: bin2hex(8 random bytes) = 16-char hex string, used as UTF-8 bytes
        byte[] randomBytes = new byte[8];
        new SecureRandom().nextBytes(randomBytes);
        String ivHex = bytesToHex(randomBytes); // 16-char hex string
        IvParameterSpec ivSpec = new IvParameterSpec(ivHex.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return ivHex + Base64.getEncoder().encodeToString(encrypted);
    }

    // -----------------------------------------------------------------------
    // 4.  AES-256-CBC decrypt
    //     Matches decrypt() in both JSP files.
    //     Input format:  first 16 chars = IV (as UTF-8 bytes), rest = Base64
    // -----------------------------------------------------------------------
    public String decrypt(String requestData, String secretKey) throws Exception {
        byte[] iv           = requestData.substring(0, 16).getBytes(StandardCharsets.UTF_8);
        String encryptedB64 = requestData.substring(16);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec  keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedB64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------------
    // 5.  SHA-256 private key  (salt @ "username :|: password" → SHA-256 hex)
    //     Matches encrypt_sha() in sendtoairpay.jsp.
    // -----------------------------------------------------------------------
    public String buildPrivateKey(String username, String password, String secret)
            throws NoSuchAlgorithmException {
        String toHash = secret + "@" + username + ":|:" + password;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(toHash.getBytes(StandardCharsets.UTF_8)));
    }

    // -----------------------------------------------------------------------
    // 6.  Checksum calculation
    //     Sort map by key → concatenate values → append today's date (yyyy-MM-dd)
    //     → SHA-256 hex.
    //     Matches checksumcal() in sendtoairpay.jsp.
    // -----------------------------------------------------------------------
    public String calculateChecksum(Map<String, String> data) throws NoSuchAlgorithmException {
        TreeMap<String, String> sorted = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        for (String value : sorted.values()) {
            sb.append(value);
        }
//        sb.append(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
//        sb.append(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString());
        sb.append(com.example.demo.util.LocalINDTime.now().toLocalDate().toString());

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    // -----------------------------------------------------------------------
    // 7.  CRC32 secure hash verification (for Airpay response validation)
    //     Matches the CRC32 check in responsefromairpay.jsp.
    // -----------------------------------------------------------------------
    public long computeCRC32(String input) {
        CRC32 crc32 = new CRC32();
        crc32.update(input.getBytes(StandardCharsets.UTF_8));
        return crc32.getValue();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}