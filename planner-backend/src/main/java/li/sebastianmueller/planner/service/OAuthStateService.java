package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.config.GoogleOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues and verifies short-lived OAuth state tokens.
 *
 * Format:  base64url(userId:epochSeconds) . base64url(HMAC-SHA256)
 * TTL:     10 minutes (600 seconds)
 * Key:     reuses google.oauth.encryption-key-base64 (32 bytes → valid HMAC-SHA256 key)
 */
@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private static final long   STATE_TTL_SECONDS = 600;
    private static final String HMAC_ALGO         = "HmacSHA256";

    private final GoogleOAuthProperties properties;

    /** Generates a signed state token for the given user. */
    public String generateState(UUID userId) {
        String payload        = userId + ":" + Instant.now().getEpochSecond();
        String encodedPayload = base64url(payload.getBytes(StandardCharsets.UTF_8));
        String signature      = hmac(encodedPayload);
        return encodedPayload + "." + signature;
    }

    /**
     * Verifies a state token and returns the embedded userId.
     * Returns {@code null} if the token is invalid, tampered with, or expired.
     */
    public UUID verifyState(String state) {
        if (state == null) return null;

        int dotIdx = state.lastIndexOf('.');
        if (dotIdx < 0) return null;

        String encodedPayload = state.substring(0, dotIdx);
        String signature      = state.substring(dotIdx + 1);

        // Constant-time HMAC comparison
        if (!hmac(encodedPayload).equals(signature)) return null;

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String[] parts = payload.split(":", 2);
        if (parts.length != 2) return null;

        long issuedAt;
        try {
            issuedAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (Instant.now().getEpochSecond() - issuedAt > STATE_TTL_SECONDS) return null;

        try {
            return UUID.fromString(parts[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------

    private String hmac(String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getEncryptionKeyBase64());
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGO));
            return base64url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private static String base64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
