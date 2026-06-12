package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.dto.LabelRequest;
import li.sebastianmueller.planner.dto.LabelResponse;
import li.sebastianmueller.planner.entity.Label;
import li.sebastianmueller.planner.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    @Transactional(readOnly = true)
    public List<LabelResponse> listAll() {
        return labelRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabelResponse create(LabelRequest request) {
        if (labelRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Label with name '" + request.getName() + "' already exists");
        }

        Label label = Label.builder()
                .name(request.getName())
                .color(request.getColor())
                .build();

        return toResponse(labelRepository.save(label));
    }

    public LabelResponse toResponse(Label label) {
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .createdAt(label.getCreatedAt())
                .build();
    }
}
