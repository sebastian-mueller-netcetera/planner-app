package li.sebastianmueller.planner.controller;

import jakarta.validation.Valid;
import li.sebastianmueller.planner.dto.LabelRequest;
import li.sebastianmueller.planner.dto.LabelResponse;
import li.sebastianmueller.planner.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;

    @GetMapping
    public ResponseEntity<List<LabelResponse>> list() {
        return ResponseEntity.ok(labelService.listAll());
    }

    @PostMapping
    public ResponseEntity<LabelResponse> create(@Valid @RequestBody LabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labelService.create(request));
    }
}
