package li.sebastianmueller.planner.repository;

import li.sebastianmueller.planner.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {

    Optional<Label> findByName(String name);

    List<Label> findAllByOrderByNameAsc();
}
