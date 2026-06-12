package li.sebastianmueller.planner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sprint creation.
 * If isoYear and isoWeek are omitted, the current ISO week is used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintRequest {

    private Integer isoYear;
    private Integer isoWeek;
}
