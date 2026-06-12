package li.sebastianmueller.planner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "task_calendar_syncs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_task_calendar_syncs_task_calendar",
        columnNames = {"task_id", "calendar_key"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCalendarSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "calendar_key", nullable = false)
    private String calendarKey;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Column(name = "sync_status", nullable = false)
    private String syncStatus;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_error")
    private String lastError;
}
