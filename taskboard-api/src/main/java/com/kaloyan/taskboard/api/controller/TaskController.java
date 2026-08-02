package com.kaloyan.taskboard.api.controller;

import com.kaloyan.taskboard.api.dto.TaskRequest;
import com.kaloyan.taskboard.api.dto.TaskResponse;
import com.kaloyan.taskboard.api.dto.TaskMapper;
import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public List<TaskResponse> getAllTasks() {
        return taskService.getAllTasks().stream().map(TaskResponse::new).toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return new TaskResponse(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        Task created = taskService.createTask(TaskMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(new TaskResponse(created));
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return new TaskResponse(taskService.updateTask(id, TaskMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}