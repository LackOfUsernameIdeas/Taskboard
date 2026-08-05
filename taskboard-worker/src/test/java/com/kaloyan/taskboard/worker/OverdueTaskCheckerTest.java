package com.kaloyan.taskboard.worker;

import com.kaloyan.taskboard.core.model.Task;
import com.kaloyan.taskboard.core.model.TaskStatus;
import com.kaloyan.taskboard.core.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueTaskCheckerTest {
    @Mock TaskRepository taskRepository;
    @InjectMocks OverdueTaskChecker checker;

    @Test void queriesOnlyTodoTasksDueBeforeTodayWhenNoneAreOverdue() {
        when(taskRepository.findByStatusAndDueDateBefore(eq(TaskStatus.TODO), any(LocalDate.class))).thenReturn(List.of());
        checker.checkOverdueTasks();
        verify(taskRepository).findByStatusAndDueDateBefore(eq(TaskStatus.TODO), eq(LocalDate.now()));
    }

    @Test void processesOverdueTasksWithoutChangingTheirState() {
        Task task = new Task(); task.setId(1L); task.setTitle("late"); task.setDueDate(LocalDate.now().minusDays(1));
        when(taskRepository.findByStatusAndDueDateBefore(eq(TaskStatus.TODO), any(LocalDate.class))).thenReturn(List.of(task));
        checker.checkOverdueTasks();
        verify(taskRepository).findByStatusAndDueDateBefore(eq(TaskStatus.TODO), eq(LocalDate.now()));
        verifyNoMoreInteractions(taskRepository);
    }
}
