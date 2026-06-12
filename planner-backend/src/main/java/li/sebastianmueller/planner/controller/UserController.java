package li.sebastianmueller.planner.controller;

import li.sebastianmueller.planner.dto.UserSummaryResponse;
import li.sebastianmueller.planner.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> list() {
        return ResponseEntity.ok(userService.listUsers());
    }
}
