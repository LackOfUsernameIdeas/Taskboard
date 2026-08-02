package com.kaloyan.taskboard.api.dto;

import com.kaloyan.taskboard.core.model.Task;

public class TaskMapper {

    private TaskMapper() {
    }

    public static Task toEntity(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setDueDate(request.getDueDate());
        return task;
    }
}