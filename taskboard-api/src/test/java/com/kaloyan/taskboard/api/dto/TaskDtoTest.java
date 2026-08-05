package com.kaloyan.taskboard.api.dto;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import com.kaloyan.taskboard.core.model.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TaskDtoTest {
    @Test void mapsRequestToEntityAndEntityToResponse() {
        TaskRequest request = new TaskRequest(); request.setTitle("Plan"); request.setDescription("Details"); request.setStatus(TaskStatus.DONE); request.setDueDate(LocalDate.of(2026, 9, 1));
        Task entity = TaskMapper.toEntity(request); entity.setId(4L);
        User owner = new User(); owner.setUsername("alice"); entity.setOwner(owner);
        TaskResponse response = new TaskResponse(entity);
        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getTitle()).isEqualTo("Plan");
        assertThat(response.getDescription()).isEqualTo("Details");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getOwner()).isEqualTo("alice");
    }

    @Test void responseSupportsTaskWithoutLoadedOwner() {
        Task task = new Task(); task.setTitle("orphan"); task.setStatus(TaskStatus.TODO);
        assertThat(new TaskResponse(task).getOwner()).isNull();
    }
}
