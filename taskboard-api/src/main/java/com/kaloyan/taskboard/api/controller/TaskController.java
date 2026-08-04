package com.kaloyan.taskboard.api.controller;

import com.kaloyan.taskboard.api.dto.TaskRequest;
import com.kaloyan.taskboard.api.dto.TaskResponse;
import com.kaloyan.taskboard.api.dto.TaskMapper;
import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getAllTasks(Authentication authentication) {
        return taskService.getAllTasks(authentication.getName(), isAdmin(authentication))
                .stream().map(TaskResponse::new).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id, Authentication authentication) {
        return new TaskResponse(taskService.getTaskById(id, authentication.getName(), isAdmin(authentication)));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request, Authentication authentication) {
        Task created = taskService.createTask(TaskMapper.toEntity(request), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TaskResponse(created));
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request, Authentication authentication) {
        return new TaskResponse(
                taskService.updateTask(id, TaskMapper.toEntity(request), authentication.getName(), isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
