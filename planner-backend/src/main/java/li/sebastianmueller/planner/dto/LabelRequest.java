package li.sebastianmueller.planner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LabelRequest {

    @NotBlank
    private String name;

    private String color;
}
