package com.kaloyan.taskboard.repository;

import com.kaloyan.taskboard.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}