package com.example.demo.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-CBC, reversible — uses only the JDK's built-in javax.crypto,
 * no external dependency required.
 *
 * WHY REVERSIBLE (encrypt/decrypt) INSTEAD OF ONE-WAY HASHING (bcrypt etc.):
 * Stanvee's wallet APIs (deduct / confirm) require the user's ACTUAL
 * plaintext password to be sent on every single call — it's not a normal
 * login where you just verify a hash once. So we MUST be able to get the
 * original password back out, which a hash can never give you. Hence AES
 * (reversible) instead of a hash (one-way).
 *
 * HOW STORAGE WORKS (encrypt):
 *   1. Generate a random 16-byte IV for this call.
 *   2. Encrypt plaintext with AES/CBC/PKCS5Padding using our fixed key + this IV.
 *   3. Prepend the IV to the ciphertext bytes, then Base64 the combined result.
 *   4. Store that single Base64 string in stanveeShop_user.password_enc.
 *   (Random IV per call means the same password encrypts to a different
 *   string every time it's saved — but it always decrypts back correctly.)
 *
 * HOW RETRIEVAL WORKS (decrypt) — i.e. getting the ORIGINAL password back:
 *   1. Base64-decode the stored string.
 *   2. Split it back into the first 16 bytes (IV) + the rest (ciphertext).
 *   3. Run AES/CBC/PKCS5Padding in DECRYPT_MODE with our fixed key + that IV.
 *   4. The output bytes, turned back into a String, are the ORIGINAL
 *      plaintext password — exactly as the user typed it.
 *
 * USAGE ELSEWHERE (e.g. inside WalletService, whenever the original
 * password is needed again to call Stanvee):
 *
 *   StanveeUser user = stanveeUserRepository.findByUsername(username)
 *           .orElseThrow(() -> new RuntimeException("user not found"));
 *   String originalPassword = encryptionUtil.decrypt(user.getPasswordEnc());
 *   // originalPassword is now the real password, ready to send to Stanvee.
 */
@Component
public class EncryptionUtil {

    private static final String ALGO = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    // Change this in production if you like — no external config needed to run.
    private static final String SECRET_PASSPHRASE = "StanveeWalletAesSecret_ChangeMe_2026";

    private final SecretKeySpec keySpec;

    public EncryptionUtil() {
        this.keySpec = deriveKey(SECRET_PASSPHRASE);
    }

    /** Turns any passphrase into a valid 32-byte AES-256 key via SHA-256. */
    private SecretKeySpec deriveKey(String passphrase) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(passphrase.getBytes("UTF-8")); // always 32 bytes
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    /**
     * ENCRYPT — call this when SAVING a password to the DB.
     * Input: the real plaintext password (e.g. "868526").
     * Output: a Base64 string (iv + ciphertext) safe to store in password_enc.
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * DECRYPT / RETRIEVE ORIGINAL — call this whenever you need the real
     * password back (e.g. before calling Stanvee's deduct/confirm APIs).
     * Input: the Base64 string stored in password_enc.
     * Output: the ORIGINAL plaintext password, byte-for-byte as it was
     * before encryption.
     */
    public String decrypt(String encoded) {
        try {
            // Step 1: undo the Base64 wrapping.
            byte[] combined = Base64.getDecoder().decode(encoded);

            // Step 2: split back into IV (first 16 bytes) + actual ciphertext.
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            // Step 3: run AES in decrypt mode with the same key + this IV.
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            // Step 4: the result is the original plaintext password.
            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
