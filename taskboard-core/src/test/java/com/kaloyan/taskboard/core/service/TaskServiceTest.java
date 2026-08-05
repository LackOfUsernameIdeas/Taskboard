package com.kaloyan.taskboard.core.service;

import com.kaloyan.taskboard.core.exception.TaskNotFoundException;
import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.TaskRepository;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @InjectMocks TaskService taskService;

    @Test void listsAllTasksForAdmin() {
        List<Task> tasks = List.of(task(1L, "one"));
        when(taskRepository.findAll()).thenReturn(tasks);

        assertThat(taskService.getAllTasks("admin", true)).isSameAs(tasks);
        verify(taskRepository).findAll();
        verify(taskRepository, never()).findByOwnerUsername(anyString());
    }

    @Test void listsOnlyOwnersTasksForRegularUser() {
        List<Task> tasks = List.of(task(1L, "one"));
        when(taskRepository.findByOwnerUsername("alice")).thenReturn(tasks);

        assertThat(taskService.getAllTasks("alice", false)).isSameAs(tasks);
        verify(taskRepository).findByOwnerUsername("alice");
    }

    @Test void findsAnyTaskForAdminAndOnlyOwnedTaskForUser() {
        Task task = task(7L, "secret");
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndOwnerUsername(7L, "alice")).thenReturn(Optional.of(task));

        assertThat(taskService.getTaskById(7L, "admin", true)).isSameAs(task);
        assertThat(taskService.getTaskById(7L, "alice", false)).isSameAs(task);
        verify(taskRepository).findById(7L);
        verify(taskRepository).findByIdAndOwnerUsername(7L, "alice");
    }

    @Test void rejectsMissingOrUnownedTaskWithoutLeakingIt() {
        when(taskRepository.findByIdAndOwnerUsername(7L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(7L, "alice", false))
                .isInstanceOf(TaskNotFoundException.class).hasMessage("Task not found with id: 7");
    }

    @Test void createsTaskWithAuthenticatedUserAsOwner() {
        User alice = user("alice");
        Task task = task(null, "new");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.save(task)).thenReturn(task);

        assertThat(taskService.createTask(task, "alice")).isSameAs(task);
        assertThat(task.getOwner()).isSameAs(alice);
        verify(taskRepository).save(task);
    }

    @Test void rejectsCreationWhenAuthenticatedUserNoLongerExists() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(task(null, "new"), "missing"))
                .isInstanceOf(IllegalStateException.class).hasMessage("Authenticated user not found: missing");
        verifyNoInteractions(taskRepository);
    }

    @Test void updatesAllMutableFieldsAfterAuthorization() {
        Task stored = task(4L, "old");
        Task update = task(null, "new");
        update.setDescription("updated"); update.setStatus(TaskStatus.DONE); update.setDueDate(LocalDate.of(2026, 8, 1));
        when(taskRepository.findByIdAndOwnerUsername(4L, "alice")).thenReturn(Optional.of(stored));
        when(taskRepository.save(stored)).thenReturn(stored);

        assertThat(taskService.updateTask(4L, update, "alice", false)).isSameAs(stored);
        assertThat(stored.getTitle()).isEqualTo("new");
        assertThat(stored.getDescription()).isEqualTo("updated");
        assertThat(stored.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(stored.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test void deletesExistingTaskAndRejectsMissingTask() {
        Task task = task(8L, "remove");
        when(taskRepository.findById(8L)).thenReturn(Optional.of(task));
        taskService.deleteTask(8L);
        verify(taskRepository).delete(task);

        when(taskRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.deleteTask(9L)).isInstanceOf(TaskNotFoundException.class);
    }

    private static Task task(Long id, String title) { Task task = new Task(); task.setId(id); task.setTitle(title); task.setStatus(TaskStatus.TODO); return task; }
    private static User user(String username) { User user = new User(); user.setUsername(username); return user; }
}
