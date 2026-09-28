package com.example.demo.service;

import com.example.demo.dto.StanveeAuthDTO.LoginRequest;
import com.example.demo.dto.StanveeAuthDTO.LoginResult;
import com.example.demo.entity.StanveeUser;
import com.example.demo.repository.StanveeUserRepository;
import com.example.demo.util.EncryptionUtil;
import com.example.demo.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class StanveeAuthService {

    private static final Logger log = LoggerFactory.getLogger(StanveeAuthService.class);
    private static final String STANVEE_BASE_URL = "https://stanveeservices.com/CheckLogin.aspx";
    private static final String STANVEE_TOKEN = "abUnMar5489pidlAewUF4875brlstangwewera4i5n6";

    private final RestTemplate restTemplate;
    private final StanveeUserRepository userRepository; // table: stanveeShop_user
    private final EncryptionUtil encryptionUtil;
    private final JwtUtil jwtUtil;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public StanveeAuthService(RestTemplate restTemplate,
                               StanveeUserRepository userRepository,
                               EncryptionUtil encryptionUtil,
                               JwtUtil jwtUtil) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.encryptionUtil = encryptionUtil;
        this.jwtUtil = jwtUtil;
    }

    public LoginResult login(LoginRequest req) {

        if (isBlank(req.getUsername()) || isBlank(req.getPassword())) {
            throw new IllegalArgumentException("username and password are required");
        }

        String url = UriComponentsBuilder.fromUriString(STANVEE_BASE_URL)
                .queryParam("token", STANVEE_TOKEN)
                .queryParam("Username", req.getUsername())
                .queryParam("Password", req.getPassword())
                .queryParam("action", "login")
                .toUriString();

        String rawResponse = restTemplate.getForEntity(url, String.class).getBody();

        JsonNode root = parseJsonSafely(rawResponse);
        boolean isSuccess = root != null
                && root.hasNonNull("response")
                && "OK".equalsIgnoreCase(root.get("response").asText());

        if (!isSuccess) {
            log.warn("Stanvee login failed for username={} — response: {}", req.getUsername(), rawResponse);
            // Still return the raw body as-is — same-to-same passthrough, even on failure.
            return new LoginResult(rawResponse, null);
        }

        JsonNode data = root.get("data");
        String loginId = textOrNull(data, "loginid");
        String name = textOrNull(data, "name");
        String email = textOrNull(data, "email");
        String mobileNo = textOrNull(data, "mobileno");

        // Fall back to the request username if Stanvee didn't echo one back.
        String username = loginId != null ? loginId : req.getUsername();

        // Saves/updates username, name, and the ENCRYPTED password into
        // stanveeShop_user. See saveOrUpdateUser() below for exactly how
        // the password is encrypted before storage, and how to reverse it.
        saveOrUpdateUser(username, name, email, mobileNo, req.getPassword());

        String jwt = jwtUtil.generateToken(username);

        log.info("Stanvee login success for username={}", username);

        return new LoginResult(rawResponse, jwt);
    }

    /**
     * Creates the local stanveeShop_user record on first login only. On
     * repeat logins we just bump last_login_at / refresh name+password
     * (in case they changed on Stanvee's side) — we never re-insert.
     *
     * PASSWORD STORAGE PROCESS:
     *   The plaintext password (req.getPassword() / plainPassword here) is
     *   NEVER stored as-is. It is run through encryptionUtil.encrypt(...)
     *   first, which returns an AES-256 encrypted, Base64-encoded string.
     *   That encrypted string — not the real password — is what gets saved
     *   into the password_enc column.
     */
    private void saveOrUpdateUser(String username, String name, String email, String mobileNo, String plainPassword) {
        StanveeUser user = userRepository.findByUsername(username).orElseGet(() -> {
            StanveeUser u = new StanveeUser();
            u.setUsername(username);
            u.setCreatedAt(LocalDateTime.now(IST));
            return u;
        });

        user.setName(name);
        user.setEmail(email);
        user.setMobileNo(mobileNo);
        user.setPasswordEnc(encryptionUtil.encrypt(plainPassword)); // encrypt before saving
        user.setUpdatedAt(LocalDateTime.now(IST));
        user.setLastLoginAt(LocalDateTime.now(IST));

        userRepository.save(user);
    }

    /**
     * PASSWORD RETRIEVAL PROCESS — how to get the ORIGINAL password back
     * out later (e.g. from WalletService, before calling Stanvee's
     * deduct/confirm APIs which need the real plaintext password).
     *
     * Steps:
     *   1. Look the user up by username in stanveeShop_user.
     *   2. Read the stored password_enc value (this is the AES-encrypted,
     *      Base64 string — NOT the real password).
     *   3. Pass it into encryptionUtil.decrypt(...), which reverses the
     *      exact same AES algorithm used in encrypt() and returns the
     *      ORIGINAL plaintext password, unchanged from what the user typed.
     *
     * Example:
     *   String originalPassword = getOriginalPassword("SV4189392");
     *   // originalPassword == "868526" (the real password)
     */
    public String getOriginalPassword(String username) {
        StanveeUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // This line does the actual reversal (decryption) back to plaintext.
        return encryptionUtil.decrypt(user.getPasswordEnc());
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return null;
        String v = node.get(field).asText();
        return v.isBlank() ? null : v;
    }

    private JsonNode parseJsonSafely(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return null;
        try {
            return mapper.readTree(rawResponse);
        } catch (Exception e) {
            log.warn("Stanvee login response was not valid JSON: {}", rawResponse);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
