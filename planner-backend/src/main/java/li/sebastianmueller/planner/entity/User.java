package li.sebastianmueller.planner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Reused for MVP refresh token hash storage.
     */
    @Column(name = "google_refresh_token_encrypted")
    private String googleRefreshTokenEncrypted;

    /**
     * Encrypted Google Calendar OAuth2 refresh token. Null until the user completes Google OAuth.
     */
    @Column(name = "google_calendar_token")
    private String googleCalendarToken;

    @Column(name = "google_calendar_a_id")
    private String googleCalendarAId;

    @Column(name = "google_calendar_b_id")
    private String googleCalendarBId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
