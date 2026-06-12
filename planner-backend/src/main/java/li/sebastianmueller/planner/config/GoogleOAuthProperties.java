package li.sebastianmueller.planner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from application.yml prefix "google.oauth".
 *
 * Required env vars (set in Coolify):
 *   GOOGLE_CLIENT_ID      – OAuth 2.0 client ID from Google Cloud Console
 *   GOOGLE_CLIENT_SECRET  – OAuth 2.0 client secret
 *   GOOGLE_REDIRECT_URI   – https://api.planner.sebastian-mueller.li/api/v1/integrations/google-calendar/callback
 *   FRONTEND_BASE_URL     – https://planner.sebastian-mueller.li
 *   GOOGLE_TOKEN_ENCRYPTION_KEY – output of: openssl rand -base64 32
 */
@Data
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendBaseUrl;
    private String encryptionKeyBase64;
}
