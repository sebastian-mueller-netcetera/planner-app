package li.sebastianmueller.planner.repository;

import li.sebastianmueller.planner.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    Optional<Sprint> findByIsoYearAndIsoWeek(int isoYear, int isoWeek);

    @Query("SELECT s FROM Sprint s WHERE s.startDate <= :today AND s.endDate >= :today")
    Optional<Sprint> findCurrentSprint(@Param("today") LocalDate today);
}
