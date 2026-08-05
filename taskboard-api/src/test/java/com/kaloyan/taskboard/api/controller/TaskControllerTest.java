package com.kaloyan.taskboard.api.controller;

import com.kaloyan.taskboard.api.dto.TaskRequest;
import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {
    @Mock TaskService taskService;
    @InjectMocks TaskController controller;

    @Test void usesAdminScopeForListingAndGettingTasks() {
        Task task = task(3L, "task", "alice");
        when(taskService.getAllTasks("admin", true)).thenReturn(List.of(task));
        when(taskService.getTaskById(3L, "admin", true)).thenReturn(task);
        var admin = auth("admin", "ROLE_ADMIN");

        assertThat(controller.getAllTasks(admin)).singleElement().satisfies(response -> assertThat(response.getOwner()).isEqualTo("alice"));
        assertThat(controller.getTaskById(3L, admin).getId()).isEqualTo(3L);
        verify(taskService).getAllTasks("admin", true);
        verify(taskService).getTaskById(3L, "admin", true);
    }

    @Test void createsAndUpdatesTaskUsingAuthenticatedUserAndMappedRequest() {
        TaskRequest request = request(); Task created = task(5L, "Title", "alice");
        when(taskService.createTask(any(Task.class), eq("alice"))).thenReturn(created);
        when(taskService.updateTask(eq(5L), any(Task.class), eq("alice"), eq(false))).thenReturn(created);
        var user = auth("alice", "ROLE_USER");

        assertThat(controller.createTask(request, user).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.updateTask(5L, request, user).getTitle()).isEqualTo("Title");
        verify(taskService).createTask(argThat(t -> t.getTitle().equals("Title") && t.getStatus() == TaskStatus.IN_PROGRESS && t.getOwner() == null), eq("alice"));
        verify(taskService).updateTask(eq(5L), argThat(t -> t.getDueDate().equals(LocalDate.of(2026, 8, 20))), eq("alice"), eq(false));
    }

    @Test void delegatesDeleteAndReturnsNoContent() {
        assertThat(controller.deleteTask(9L).getStatusCode().value()).isEqualTo(204);
        verify(taskService).deleteTask(9L);
    }

    private static UsernamePasswordAuthenticationToken auth(String name, String authority) { return new UsernamePasswordAuthenticationToken(name, null, List.of(new SimpleGrantedAuthority(authority))); }
    private static TaskRequest request() { TaskRequest request = new TaskRequest(); request.setTitle("Title"); request.setDescription("Description"); request.setStatus(TaskStatus.IN_PROGRESS); request.setDueDate(LocalDate.of(2026, 8, 20)); return request; }
    private static Task task(Long id, String title, String ownerName) { User owner = new User(); owner.setUsername(ownerName); Task task = new Task(); task.setId(id); task.setTitle(title); task.setOwner(owner); task.setStatus(TaskStatus.TODO); return task; }
}
