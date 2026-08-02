package com.kaloyan.taskboard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.kaloyan.taskboard")
@EntityScan(basePackages = "com.kaloyan.taskboard.core.model")
@EnableJpaRepositories(basePackages = "com.kaloyan.taskboard.core.repository")
public class TaskboardApplication {
	public static void main(String[] args) {
		SpringApplication.run(TaskboardApplication.class, args);
	}
}