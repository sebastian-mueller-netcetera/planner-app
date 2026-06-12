package li.sebastianmueller.planner.controller;

import li.sebastianmueller.planner.config.GoogleOAuthProperties;
import li.sebastianmueller.planner.service.GoogleTokenEncryptionService;
import li.sebastianmueller.planner.service.OAuthStateService;
import li.sebastianmueller.planner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Google Calendar OAuth2 authorization-code flow.
 *
 * GET /api/v1/integrations/google-calendar/connect  – authenticated – returns { authorizationUrl }
 * GET /api/v1/integrations/google-calendar/callback – public       – exchanges code, stores token, redirects
 * GET /api/v1/integrations/google-calendar/status   – authenticated – returns { connected: true/false }
 */
@RestController
@RequestMapping("/api/v1/integrations/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarOAuthController {

    private static final String GOOGLE_AUTH_URL  = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_SCOPE   = "https://www.googleapis.com/auth/calendar";

    private final GoogleOAuthProperties        properties;
    private final OAuthStateService            oAuthStateService;
    private final GoogleTokenEncryptionService encryptionService;
    private final UserService                  userService;

    // ------------------------------------------------------------------
    // 1. Connect – returns the Google authorization URL to redirect to
    // ------------------------------------------------------------------

    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(
            @AuthenticationPrincipal UserDetails principal) {

        UUID   userId = UUID.fromString(principal.getUsername());
        String state  = oAuthStateService.generateState(userId);

        String authorizationUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id",     properties.getClientId())
                .queryParam("redirect_uri",  properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope",         CALENDAR_SCOPE)
                .queryParam("access_type",   "offline")
                .queryParam("prompt",        "consent")
                .queryParam("state",         state)
                .build().toUriString();

        return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }

    // ------------------------------------------------------------------
    // 2. Callback – public endpoint; Google redirects here with ?code=&state=
    // ------------------------------------------------------------------

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        String frontendBase = properties.getFrontendBaseUrl();

        // OAuth error or missing params
        if (error != null || code == null || state == null) {
            return redirect(frontendBase + "/settings?googleCalendar=error");
        }

        // Verify state token (tamper-proof + TTL)
        UUID userId = oAuthStateService.verifyState(state);
        if (userId == null) {
            return redirect(frontendBase + "/settings?googleCalendar=error");
        }

        // Exchange authorization code for tokens
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code",          code);
            form.add("client_id",     properties.getClientId());
            form.add("client_secret", properties.getClientSecret());
            form.add("redirect_uri",  properties.getRedirectUri());
            form.add("grant_type",    "authorization_code");

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = RestClient.create()
                    .post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("refresh_token")) {
                // No refresh token – happens when prompt=consent was not respected
                return redirect(frontendBase + "/settings?googleCalendar=error");
            }

            String encryptedToken = encryptionService.encrypt((String) tokenResponse.get("refresh_token"));
            userService.updateGoogleRefreshToken(userId, encryptedToken);

            return redirect(frontendBase + "/settings?googleCalendar=connected");

        } catch (Exception e) {
            return redirect(frontendBase + "/settings?googleCalendar=error");
        }
    }

    // ------------------------------------------------------------------
    // 3. Status – returns whether the user has a connected Google Calendar
    // ------------------------------------------------------------------

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(
            @AuthenticationPrincipal UserDetails principal) {

        UUID    userId    = UUID.fromString(principal.getUsername());
        boolean connected = userService.hasGoogleRefreshToken(userId);
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    // ------------------------------------------------------------------

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }
}
