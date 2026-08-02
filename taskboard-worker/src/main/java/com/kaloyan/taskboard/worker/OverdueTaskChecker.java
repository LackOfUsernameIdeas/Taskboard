package com.kaloyan.taskboard.worker;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import com.kaloyan.taskboard.core.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueTaskChecker {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskChecker.class);

    private final TaskRepository taskRepository;

    public OverdueTaskChecker(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void checkOverdueTasks() {
        List<Task> overdueTasks = taskRepository.findByStatusAndDueDateBefore(TaskStatus.TODO, LocalDate.now());

        if (overdueTasks.isEmpty()) {
            log.info("No overdue tasks found.");
            return;
        }

        for (Task task : overdueTasks) {
            log.warn("Overdue task: [id={}] '{}' was due on {}", task.getId(), task.getTitle(), task.getDueDate());
        }
    }
}