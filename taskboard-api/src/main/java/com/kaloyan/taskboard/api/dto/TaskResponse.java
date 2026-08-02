package com.kaloyan.taskboard.api.dto;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;

import java.time.LocalDate;

public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final TaskStatus status;
    private final LocalDate dueDate;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.dueDate = task.getDueDate();
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

    public LocalDate getDueDate() {
        return dueDate;
    }
}