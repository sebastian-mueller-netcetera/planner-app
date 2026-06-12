package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.dto.UserSummaryResponse;
import li.sebastianmueller.planner.entity.User;
import li.sebastianmueller.planner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "displayName"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserSummaryResponse toResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }
}
