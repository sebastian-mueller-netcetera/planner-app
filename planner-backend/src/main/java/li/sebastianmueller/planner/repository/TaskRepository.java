package li.sebastianmueller.planner.repository;

import li.sebastianmueller.planner.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    /**
     * Find active (non-archived) task by id.
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.archivedAt IS NULL")
    Optional<Task> findActiveById(@Param("id") UUID id);

    /**
     * Full-text search using PostgreSQL GIN index (German dictionary).
     * Falls back gracefully to ILIKE if tsvector match fails.
     */
    @Query(value = """
            SELECT * FROM tasks t
            WHERE t.archived_at IS NULL
              AND to_tsvector('german', t.title || ' ' || COALESCE(t.description, ''))
                  @@ plainto_tsquery('german', :query)
            """,
           countQuery = """
            SELECT COUNT(*) FROM tasks t
            WHERE t.archived_at IS NULL
              AND to_tsvector('german', t.title || ' ' || COALESCE(t.description, ''))
                  @@ plainto_tsquery('german', :query)
            """,
           nativeQuery = true)
    Page<Task> fullTextSearch(@Param("query") String query, Pageable pageable);

    /**
     * ILIKE fallback search (used when full-text query returns no results or as alternative).
     */
    @Query("SELECT t FROM Task t WHERE t.archivedAt IS NULL AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Task> searchByTitleOrDescriptionIgnoreCase(@Param("query") String query, Pageable pageable);
}
