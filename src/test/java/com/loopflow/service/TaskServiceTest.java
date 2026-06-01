package com.loopflow.service;

import com.loopflow.dao.TaskDAO;
import com.loopflow.dao.TaskHistoryDAO;
import com.loopflow.model.Task;
import com.loopflow.model.TaskHistory;
import com.loopflow.model.enums.Priority;
import com.loopflow.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link TaskService} usando Mockito.
 * Verifica especialmente la lógica de {@link TaskService#moveTask}.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskDAO taskDAO;
    @Mock private TaskHistoryDAO taskHistoryDAO;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskDAO, taskHistoryDAO);
    }

    // ===========================
    // Tests de createTask
    // ===========================

    @Test
    @DisplayName("createTask() guarda la tarea y registra creación en historial")
    void testCreateTask_savesAndRecordsHistory() {
        Task input = new Task("Nueva Tarea", "Desc", null, null, null);
        Task savedTask = buildTask(1, "Nueva Tarea", TaskStatus.TODO, Priority.MEDIUM);

        when(taskDAO.save(any())).thenReturn(savedTask);
        when(taskHistoryDAO.save(any())).thenReturn(new TaskHistory());

        Task result = service.createTask(input);

        assertNotNull(result);
        assertEquals(1, result.getId());

        // Verificar que se registró en el historial con old_status = null
        ArgumentCaptor<TaskHistory> historyCaptor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryDAO, times(1)).save(historyCaptor.capture());

        TaskHistory capturedHistory = historyCaptor.getValue();
        assertNull(capturedHistory.getOldStatus(), "El estado anterior debe ser null en creación");
        assertEquals(TaskStatus.TODO, capturedHistory.getNewStatus());
    }

    @Test
    @DisplayName("createTask() lanza excepción si título está vacío")
    void testCreateTask_emptyTitle() {
        Task task = new Task("", null, TaskStatus.TODO, Priority.MEDIUM, null);

        assertThrows(IllegalArgumentException.class, () -> service.createTask(task));
        verify(taskDAO, never()).save(any());
        verify(taskHistoryDAO, never()).save(any());
    }

    // ===========================
    // Tests de moveTask
    // ===========================

    @Test
    @DisplayName("moveTask() cambia estado de TODO a IN_PROGRESS y registra historial")
    void testMoveTask_todoToInProgress() {
        Task existing = buildTask(1, "Tarea", TaskStatus.TODO, Priority.MEDIUM);
        Task updated  = buildTask(1, "Tarea", TaskStatus.IN_PROGRESS, Priority.MEDIUM);

        when(taskDAO.findById(1))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(updated));
        when(taskDAO.updateStatus(1, TaskStatus.IN_PROGRESS)).thenReturn(true);
        when(taskHistoryDAO.save(any())).thenReturn(new TaskHistory());

        Task result = service.moveTask(1, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());

        // Verificar historial
        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryDAO, times(1)).save(captor.capture());
        TaskHistory history = captor.getValue();
        assertEquals(TaskStatus.TODO, history.getOldStatus());
        assertEquals(TaskStatus.IN_PROGRESS, history.getNewStatus());
        assertTrue(history.getNotes().contains("TODO"));
        assertTrue(history.getNotes().contains("IN_PROGRESS"));
    }

    @Test
    @DisplayName("moveTask() lanza excepción si la tarea ya está en el estado destino")
    void testMoveTask_sameStatus() {
        Task task = buildTask(1, "Tarea", TaskStatus.DONE, Priority.LOW);
        when(taskDAO.findById(1)).thenReturn(Optional.of(task));

        assertThrows(IllegalArgumentException.class,
                () -> service.moveTask(1, TaskStatus.DONE));
        verify(taskDAO, never()).updateStatus(anyInt(), any());
        verify(taskHistoryDAO, never()).save(any());
    }

    @Test
    @DisplayName("moveTask() lanza excepción si la tarea no existe")
    void testMoveTask_taskNotFound() {
        when(taskDAO.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.moveTask(999, TaskStatus.DONE));
    }

    @Test
    @DisplayName("moveTask() lanza excepción si newStatus es null")
    void testMoveTask_nullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.moveTask(1, null));
        verify(taskDAO, never()).findById(anyInt());
    }

    // ===========================
    // Tests de deleteTask
    // ===========================

    @Test
    @DisplayName("deleteTask() elimina la tarea cuando existe")
    void testDeleteTask_success() {
        Task task = buildTask(1, "Tarea", TaskStatus.TODO, Priority.MEDIUM);
        when(taskDAO.findById(1)).thenReturn(Optional.of(task));
        when(taskDAO.delete(1)).thenReturn(true);

        assertDoesNotThrow(() -> service.deleteTask(1));
        verify(taskDAO, times(1)).delete(1);
    }

    @Test
    @DisplayName("deleteTask() lanza excepción si la tarea no existe")
    void testDeleteTask_notFound() {
        when(taskDAO.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteTask(999));
        verify(taskDAO, never()).delete(anyInt());
    }

    // ===========================
    // Helpers
    // ===========================

    private Task buildTask(int id, String title, TaskStatus status, Priority priority) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(priority);
        return t;
    }
}
