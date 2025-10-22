package com.example.xp.controllers;

import com.example.xp.entity.Task;
import com.example.xp.entity.User;
import com.example.xp.repository.TaskRepository;
import com.example.xp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String showHomePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        List<Task> tasks = taskRepository.findByUserOrderByIdDesc(user);
        // Сортируем задачи: невыполненные сначала, выполненные в конце
        tasks.sort((t1, t2) -> {
            boolean isDone1 = t1.getStatus() == Task.TaskStatus.COMPLETED;
            boolean isDone2 = t2.getStatus() == Task.TaskStatus.COMPLETED;
            if (isDone1 && !isDone2) return 1;
            if (!isDone1 && isDone2) return -1;
            return t2.getId().compareTo(t1.getId());
        });
        
        model.addAttribute("tasks", tasks);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("newTask", new Task());
        model.addAttribute("statuses", Task.TaskStatus.values());

        return "index";
    }

    @PostMapping("/tasks/add")
    public String addTask(@ModelAttribute Task task, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            task.setUser(user);
            if (task.getStatus() == null) {
                task.setStatus(Task.TaskStatus.NOT_STARTED);
            }
            taskRepository.save(task);
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Task task = taskRepository.findById(id).orElse(null);
        if (task != null && task.getUser().getId().equals(userId)) {
            task.setStatus(Task.TaskStatus.valueOf(status));
            taskRepository.save(task);
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Task task = taskRepository.findById(id).orElse(null);
        if (task != null && task.getUser().getId().equals(userId)) {
            taskRepository.delete(task);
        }

        return "redirect:/";
    }

    @GetMapping("/task/edit/{id}")
    public String showEditTaskForm(@PathVariable Long id, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent() && task.get().getUser().getId().equals(userId)) {
            model.addAttribute("task", task.get());
            return "edit-task";
        }
        return "redirect:/";
    }

    @PostMapping("/task/edit/{id}")
    public String processEditTask(@PathVariable Long id, 
                                 @RequestParam String title,
                                 @RequestParam(required = false) String deadline,
                                 HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isPresent() && taskOpt.get().getUser().getId().equals(userId)) {
            Task task = taskOpt.get();
            task.setTitle(title);
            if (deadline != null && !deadline.isEmpty()) {
                task.setDeadline(java.time.LocalDate.parse(deadline));
            } else {
                task.setDeadline(null);
            }
            taskRepository.save(task);
        }
        return "redirect:/";
    }
}
