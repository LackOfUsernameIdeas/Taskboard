package com.kaloyan.taskboard.core.repository;

import com.kaloyan.taskboard.core.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}