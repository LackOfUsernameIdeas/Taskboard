package com.kaloyan.taskboard.core.repository;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatusAndDueDateBefore(TaskStatus status, LocalDate date);

    List<Task> findByOwnerUsername(String username);

    Optional<Task> findByIdAndOwnerUsername(Long id, String username);
}