package com.kaloyan.taskboard.dto;

import com.kaloyan.taskboard.model.Task;
import com.kaloyan.taskboard.model.TaskStatus;

public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final TaskStatus status;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }
}