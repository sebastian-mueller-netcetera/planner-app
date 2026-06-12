package li.sebastianmueller.planner.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import li.sebastianmueller.planner.entity.Label;
import li.sebastianmueller.planner.entity.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Specifications for dynamic Task filtering.
 * Always combine with {@link #notArchived()} as baseline.
 */
public final class TaskSpecification {

    private TaskSpecification() {}

    public static Specification<Task> notArchived() {
        return (root, query, cb) -> cb.isNull(root.get("archivedAt"));
    }

    public static Specification<Task> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasAssignee(UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> hasLabel(UUID labelId) {
        return (root, query, cb) -> {
            Join<Task, Label> labels = root.join("labels", JoinType.INNER);
            query.distinct(true);
            return cb.equal(labels.get("id"), labelId);
        };
    }

    public static Specification<Task> hasSprint(UUID sprintId) {
        return (root, query, cb) -> cb.equal(root.get("sprint").get("id"), sprintId);
    }

    public static Specification<Task> noSprint() {
        return (root, query, cb) -> cb.isNull(root.get("sprint"));
    }

    public static Specification<Task> hasNoDueDate() {
        return (root, query, cb) -> cb.isNull(root.get("dueDate"));
    }

    public static Specification<Task> hasDueDate() {
        return (root, query, cb) -> cb.isNotNull(root.get("dueDate"));
    }

    public static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), date);
    }

    public static Specification<Task> titleOrDescriptionContains(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("description")), pattern)
        );
    }
}
