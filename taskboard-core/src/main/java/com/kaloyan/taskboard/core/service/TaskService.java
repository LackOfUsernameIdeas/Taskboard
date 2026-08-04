package com.kaloyan.taskboard.core.service;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.TaskRepository;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.kaloyan.taskboard.core.exception.TaskNotFoundException;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Admins see every task; regular users only see their own.
     */
    public List<Task> getAllTasks(String requestingUsername, boolean isAdmin) {
        if (isAdmin) {
            return taskRepository.findAll();
        }
        return taskRepository.findByOwnerUsername(requestingUsername);
    }

    public Task getTaskById(Long id, String requestingUsername, boolean isAdmin) {
        if (isAdmin) {
            return taskRepository.findById(id)
                    .orElseThrow(() -> new TaskNotFoundException(id));
        }
        return taskRepository.findByIdAndOwnerUsername(id, requestingUsername)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task createTask(Task task, String requestingUsername) {
        User owner = userRepository.findByUsername(requestingUsername)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found: " + requestingUsername));
        task.setOwner(owner);
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task updatedTask, String requestingUsername, boolean isAdmin) {
        Task task = getTaskById(id, requestingUsername, isAdmin);
        task.setTitle(updatedTask.getTitle());
        task.setDescription(updatedTask.getDescription());
        task.setStatus(updatedTask.getStatus());
        task.setDueDate(updatedTask.getDueDate());
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }
}
