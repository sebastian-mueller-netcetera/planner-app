package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.config.GoogleOAuthProperties;
import li.sebastianmueller.planner.entity.Task;
import li.sebastianmueller.planner.entity.TaskCalendarSync;
import li.sebastianmueller.planner.entity.User;
import li.sebastianmueller.planner.repository.TaskCalendarSyncRepository;
import li.sebastianmueller.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private final UserRepository userRepository;
    private final GoogleTokenEncryptionService encryptionService;
    private final GoogleOAuthProperties oAuthProperties;
    private final TaskCalendarSyncRepository syncRepository;

    // Initialized inline — not included in @RequiredArgsConstructor constructor
    private final RestClient restClient = RestClient.create();

    /**
     * Sync or desync task based on its current toggle state.
     * - Toggle ON + calendarId set  → create or update Google Calendar event
     * - Toggle OFF                  → delete event if one was previously created
     *
     * Best-effort: never throws. Called after task create/update.
     */
    public void syncTask(Task task, UUID userId) {
        User user = resolveUser(userId);
        if (user == null) return;

        String accessToken = mintAccessToken(user.getGoogleCalendarToken());
        if (accessToken == null) return;

        handleCalendar(task, "A",
                Boolean.TRUE.equals(task.getSyncGoogleCalendarA()),
                user.getGoogleCalendarAId(),
                accessToken);

        handleCalendar(task, "B",
                Boolean.TRUE.equals(task.getSyncGoogleCalendarB()),
                user.getGoogleCalendarBId(),
                accessToken);
    }

    /**
     * Remove the task from all calendars it was synced to.
     * Best-effort: never throws. Called before task delete.
     */
    public void desyncTask(Task task, UUID userId) {
        User user = resolveUser(userId);
        if (user == null) return;

        String accessToken = mintAccessToken(user.getGoogleCalendarToken());
        if (accessToken == null) return;

        desyncFromCalendar(task, user.getGoogleCalendarAId(), "A", accessToken);
        desyncFromCalendar(task, user.getGoogleCalendarBId(), "B", accessToken);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void handleCalendar(Task task, String calendarKey,
                                boolean syncEnabled, String calendarId, String accessToken) {
        if (syncEnabled) {
            if (calendarId != null && !calendarId.isBlank()) {
                syncToCalendar(task, calendarId, calendarKey, accessToken);
            }
        } else {
            desyncFromCalendar(task, calendarId, calendarKey, accessToken);
        }
    }

    private void syncToCalendar(Task task, String calendarId,
                                String calendarKey, String accessToken) {
        try {
            Optional<TaskCalendarSync> existing =
                    syncRepository.findByTaskIdAndCalendarKey(task.getId(), calendarKey);

            boolean hasEventId = existing.isPresent()
                    && existing.get().getExternalEventId() != null
                    && !existing.get().getExternalEventId().isBlank();

            Map<String, Object> eventBody = buildEventBody(task);

            if (hasEventId) {
                // PATCH — update existing event
                String eventId = existing.get().getExternalEventId();
                restClient.patch()
                        .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                                calendarId, eventId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(eventBody)
                        .retrieve()
                        .toBodilessEntity();

                TaskCalendarSync sync = existing.get();
                sync.setSyncStatus("SYNCED");
                sync.setLastSyncedAt(OffsetDateTime.now());
                sync.setLastError(null);
                syncRepository.save(sync);

            } else {
                // POST — create new event
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) restClient.post()
                        .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events",
                                calendarId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(eventBody)
                        .retrieve()
                        .body(Map.class);

                String eventId = response != null ? (String) response.get("id") : null;
                if (eventId == null) {
                    throw new RuntimeException("Google Calendar API returned no event id");
                }

                TaskCalendarSync sync = existing.orElseGet(() -> TaskCalendarSync.builder()
                        .task(task)
                        .calendarKey(calendarKey)
                        .externalEventId(eventId)
                        .build());
                sync.setExternalEventId(eventId);
                sync.setSyncStatus("SYNCED");
                sync.setLastSyncedAt(OffsetDateTime.now());
                sync.setLastError(null);
                syncRepository.save(sync);
            }

        } catch (Exception e) {
            log.error("Failed to sync task {} to calendar {}: {}", task.getId(), calendarKey, e.getMessage(), e);
            saveSyncFailure(task, calendarKey, e.getMessage());
        }
    }

    private void desyncFromCalendar(Task task, String calendarId,
                                    String calendarKey, String accessToken) {
        try {
            Optional<TaskCalendarSync> existing =
                    syncRepository.findByTaskIdAndCalendarKey(task.getId(), calendarKey);
            if (existing.isEmpty()) return;

            String eventId = existing.get().getExternalEventId();
            if (eventId != null && !eventId.isBlank()
                    && calendarId != null && !calendarId.isBlank()) {
                restClient.delete()
                        .uri("https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}",
                                calendarId, eventId)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .toBodilessEntity();
            }

            syncRepository.delete(existing.get());

        } catch (Exception e) {
            log.error("Failed to desync task {} from calendar {}: {}", task.getId(), calendarKey, e.getMessage(), e);
            // Best-effort — swallow
        }
    }

    private Map<String, Object> buildEventBody(Task task) {
        String dateStr = task.getDueDate() != null
                ? task.getDueDate().toString()
                : LocalDate.now().toString();

        Map<String, String> dateMap = Map.of("date", dateStr);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", task.getTitle());
        if (task.getDescription() != null) {
            event.put("description", task.getDescription());
        }
        event.put("start", dateMap);
        event.put("end", dateMap);
        return event;
    }

    private String mintAccessToken(String encryptedRefreshToken) {
        if (encryptedRefreshToken == null || encryptedRefreshToken.isBlank()) return null;
        try {
            String refreshToken = encryptionService.decrypt(encryptedRefreshToken);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("client_id", oAuthProperties.getClientId());
            params.add("client_secret", oAuthProperties.getClientSecret());
            params.add("refresh_token", refreshToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(Map.class);

            return response != null ? (String) response.get("access_token") : null;

        } catch (Exception e) {
            log.error("Failed to mint Google access token: {}", e.getMessage(), e);
            return null;
        }
    }

    /** Returns null if user has no Google Calendar token (skip sync silently). */
    private User resolveUser(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null
                || user.getGoogleCalendarToken() == null
                || user.getGoogleCalendarToken().isBlank()) {
            return null;
        }
        return user;
    }

    /** Persist a FAILED sync record without throwing. */
    private void saveSyncFailure(Task task, String calendarKey, String errorMessage) {
        try {
            TaskCalendarSync sync = syncRepository
                    .findByTaskIdAndCalendarKey(task.getId(), calendarKey)
                    .orElseGet(() -> TaskCalendarSync.builder()
                            .task(task)
                            .calendarKey(calendarKey)
                            .externalEventId("")   // satisfies NOT NULL constraint
                            .build());
            sync.setSyncStatus("FAILED");
            sync.setLastError(errorMessage != null
                    ? errorMessage.substring(0, Math.min(errorMessage.length(), 500))
                    : "Unknown error");
            sync.setLastSyncedAt(OffsetDateTime.now());
            syncRepository.save(sync);
        } catch (Exception ex) {
            log.error("Failed to persist sync-failure record for task {}: {}", task.getId(), ex.getMessage(), ex);
        }
    }
}
