package li.sebastianmueller.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintSummaryResponse {

    private UUID id;
    private String name;
    private Integer isoYear;
    private Integer isoWeek;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
