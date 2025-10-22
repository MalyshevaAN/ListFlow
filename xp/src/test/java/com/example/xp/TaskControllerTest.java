package com.example.xp;

import com.example.xp.entity.Task;
import com.example.xp.entity.User;
import com.example.xp.repository.TaskRepository;
import com.example.xp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private MockHttpSession session;

    @BeforeEach
    public void setup() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser = userRepository.save(testUser);

        session = new MockHttpSession();
        session.setAttribute("userId", testUser.getId());
    }

    @Test
    public void testAddTask() throws Exception {
        mockMvc.perform(post("/tasks/add")
                        .session(session)
                        .param("title", "Новая задача")
                        .param("deadline", "2024-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        List<Task> tasks = taskRepository.findByUserOrderByIdDesc(testUser);
        assertEquals(1, tasks.size());
        assertEquals("Новая задача", tasks.get(0).getTitle());
        assertEquals(LocalDate.parse("2024-12-31"), tasks.get(0).getDeadline());
    }

    @Test
    public void testAddTaskWithoutDeadline() throws Exception {
        mockMvc.perform(post("/tasks/add")
                        .session(session)
                        .param("title", "Задача без дедлайна"))
                .andExpect(status().is3xxRedirection());

        List<Task> tasks = taskRepository.findByUserOrderByIdDesc(testUser);
        assertEquals(1, tasks.size());
        assertNull(tasks.get(0).getDeadline());
    }

    @Test
    public void testUpdateTaskStatus() throws Exception {
        Task task = new Task();
        task.setTitle("Тестовая задача");
        task.setUser(testUser);
        task.setStatus(Task.TaskStatus.NOT_STARTED);
        task = taskRepository.save(task);

        mockMvc.perform(post("/tasks/" + task.getId() + "/status")
                        .session(session)
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection());

        Task updatedTask = taskRepository.findById(task.getId()).orElse(null);
        assertNotNull(updatedTask);
        assertEquals(Task.TaskStatus.IN_PROGRESS, updatedTask.getStatus());
    }

    @Test
    public void testCompletedTasksMovedToBottom() throws Exception {
        Task task1 = new Task();
        task1.setTitle("Задача 1");
        task1.setUser(testUser);
        task1.setStatus(Task.TaskStatus.NOT_STARTED);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Задача 2");
        task2.setUser(testUser);
        task2.setStatus(Task.TaskStatus.COMPLETED);
        taskRepository.save(task2);

        Task task3 = new Task();
        task3.setTitle("Задача 3");
        task3.setUser(testUser);
        task3.setStatus(Task.TaskStatus.IN_PROGRESS);
        taskRepository.save(task3);

        List<Task> tasks = taskRepository.findByUserOrderByIdDesc(testUser);
        tasks.sort((t1, t2) -> {
            boolean isDone1 = t1.getStatus() == Task.TaskStatus.COMPLETED;
            boolean isDone2 = t2.getStatus() == Task.TaskStatus.COMPLETED;
            if (isDone1 && !isDone2) return 1;
            if (!isDone1 && isDone2) return -1;
            return t2.getId().compareTo(t1.getId());
        });

        assertEquals(3, tasks.size());
        assertNotEquals(Task.TaskStatus.COMPLETED, tasks.get(0).getStatus());
        assertNotEquals(Task.TaskStatus.COMPLETED, tasks.get(1).getStatus());
        assertEquals(Task.TaskStatus.COMPLETED, tasks.get(2).getStatus());
    }

    @Test
    public void testDeleteTask() throws Exception {
        Task task = new Task();
        task.setTitle("Задача для удаления");
        task.setUser(testUser);
        task.setStatus(Task.TaskStatus.NOT_STARTED);
        task = taskRepository.save(task);

        Long taskId = task.getId();

        mockMvc.perform(post("/tasks/" + taskId + "/delete")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        assertFalse(taskRepository.findById(taskId).isPresent());
    }

    @Test
    public void testEditTask() throws Exception {
        Task task = new Task();
        task.setTitle("Старое название");
        task.setUser(testUser);
        task.setDeadline(LocalDate.parse("2024-12-31"));
        task = taskRepository.save(task);

        mockMvc.perform(post("/task/edit/" + task.getId())
                        .session(session)
                        .param("title", "Новое название")
                        .param("deadline", "2025-01-15"))
                .andExpect(status().is3xxRedirection());

        Task updatedTask = taskRepository.findById(task.getId()).orElse(null);
        assertNotNull(updatedTask);
        assertEquals("Новое название", updatedTask.getTitle());
        assertEquals(LocalDate.parse("2025-01-15"), updatedTask.getDeadline());
    }

    @Test
    public void testUserCannotAccessOtherUsersTasks() throws Exception {
        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password456");
        anotherUser = userRepository.save(anotherUser);

        Task task = new Task();
        task.setTitle("Чужая задача");
        task.setUser(anotherUser);
        task.setStatus(Task.TaskStatus.NOT_STARTED);
        task = taskRepository.save(task);

        Long taskId = task.getId();

        mockMvc.perform(post("/tasks/" + taskId + "/delete")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        assertTrue(taskRepository.findById(taskId).isPresent());
    }
}
