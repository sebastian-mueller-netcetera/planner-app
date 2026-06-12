package li.sebastianmueller.planner.service;

import li.sebastianmueller.planner.dto.*;
import li.sebastianmueller.planner.entity.*;
import li.sebastianmueller.planner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final LabelRepository labelRepository;
    private final CommentRepository commentRepository;
    private final LabelService labelService;

    @Transactional(readOnly = true)
    public PagedResponse<TaskListItemResponse> list(
            String search,
            String status,
            UUID assigneeId,
            UUID labelId,
            UUID sprintId,
            Boolean hasDueDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage;

        if (search != null && !search.isBlank()) {
            // Full-text search via native query
            taskPage = taskRepository.fullTextSearch(search.trim(), pageable);
        } else {
            // Specification-based filtered listing
            Specification<Task> spec = TaskSpecification.notArchived();

            if (status != null && !status.isBlank()) {
                spec = spec.and(TaskSpecification.hasStatus(status));
            }
            if (assigneeId != null) {
                spec = spec.and(TaskSpecification.hasAssignee(assigneeId));
            }
            if (labelId != null) {
                spec = spec.and(TaskSpecification.hasLabel(labelId));
            }
            if (sprintId != null) {
                spec = spec.and(TaskSpecification.hasSprint(sprintId));
            }
            if (hasDueDate != null) {
                spec = spec.and(hasDueDate
                        ? TaskSpecification.hasDueDate()
                        : TaskSpecification.hasNoDueDate());
            }

            taskPage = taskRepository.findAll(spec, pageable);
        }

        List<TaskListItemResponse> content = taskPage.getContent()
                .stream()
                .map(this::toListItemResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TaskListItemResponse>builder()
                .content(content)
                .page(taskPage.getNumber())
                .size(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .last(taskPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id) {
        Task task = taskRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return toDetailResponse(task);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task.TaskBuilder builder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : "BACKLOG")
                .syncGoogleCalendarA(Boolean.TRUE.equals(request.getSyncGoogleCalendarA()))
                .syncGoogleCalendarB(Boolean.TRUE.equals(request.getSyncGoogleCalendarB()))
                .dueDate(request.getDueDate());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));
            builder.assignee(assignee);
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint not found"));
            builder.sprint(sprint);
        }

        Task task = builder.build();

        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            Set<Label> labels = new HashSet<>(labelRepository.findAllById(request.getLabelIds()));
            task.setLabels(labels);
        }

        return toDetailResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = taskRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getSyncGoogleCalendarA() != null) task.setSyncGoogleCalendarA(request.getSyncGoogleCalendarA());
        if (request.getSyncGoogleCalendarB() != null) task.setSyncGoogleCalendarB(request.getSyncGoogleCalendarB());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee not found"));
            task.setAssignee(assignee);
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sprint not found"));
            task.setSprint(sprint);
        }

        if (request.getLabelIds() != null) {
            Set<Label> labels = new HashSet<>(labelRepository.findAllById(request.getLabelIds()));
            task.setLabels(labels);
        }

        return toDetailResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID id) {
        Task task = taskRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        task.setArchivedAt(OffsetDateTime.now());
        taskRepository.save(task);
    }

    @Transactional
    public CommentResponse addComment(UUID taskId, CommentRequest request) {
        Task task = taskRepository.findActiveById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User author = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Comment comment = Comment.builder()
                .task(task)
                .author(author)
                .body(request.getBody())
                .build();

        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private TaskListItemResponse toListItemResponse(Task task) {
        return TaskListItemResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignee(task.getAssignee() != null ? toUserSummary(task.getAssignee()) : null)
                .dueDate(task.getDueDate())
                .sprint(task.getSprint() != null ? toSprintSummary(task.getSprint()) : null)
                .syncGoogleCalendarA(task.getSyncGoogleCalendarA())
                .syncGoogleCalendarB(task.getSyncGoogleCalendarB())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .labels(task.getLabels().stream()
                        .map(labelService::toResponse)
                        .collect(Collectors.toSet()))
                .build();
    }

    private TaskResponse toDetailResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignee(task.getAssignee() != null ? toUserSummary(task.getAssignee()) : null)
                .dueDate(task.getDueDate())
                .sprint(task.getSprint() != null ? toSprintSummary(task.getSprint()) : null)
                .syncGoogleCalendarA(task.getSyncGoogleCalendarA())
                .syncGoogleCalendarB(task.getSyncGoogleCalendarB())
                .archivedAt(task.getArchivedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .labels(task.getLabels().stream()
                        .map(labelService::toResponse)
                        .collect(Collectors.toSet()))
                .comments(task.getComments().stream()
                        .map(this::toCommentResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .body(comment.getBody())
                .author(toUserSummary(comment.getAuthor()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    private SprintSummaryResponse toSprintSummary(Sprint sprint) {
        return SprintSummaryResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .status(sprint.getStatus())
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        return switch (sortBy != null ? sortBy : "createdAt") {
            case "dueDate" -> Sort.by(Sort.Order.asc("dueDate").nullsLast())
                                   .and(Sort.by(direction, "createdAt"));
            case "title" -> Sort.by(direction, "title");
            default -> Sort.by(direction, "createdAt");
        };
    }
}
