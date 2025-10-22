package com.example.xp.controllers;

import com.example.xp.entity.Task;
import com.example.xp.entity.User;
import com.example.xp.entity.Folder;
import com.example.xp.repository.TaskRepository;
import com.example.xp.repository.UserRepository;
import com.example.xp.repository.FolderRepository;
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
    
    @Autowired
    private FolderRepository folderRepository;

    @GetMapping("/")
    public String showHomePage(HttpSession session, Model model, 
                              @RequestParam(required = false) Long folderId) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        Folder defaultFolder = folderRepository.findByUserAndIsDefault(user, true)
                .orElseGet(() -> {
                    Folder folder = new Folder();
                    folder.setName("Главное");
                    folder.setUser(user);
                    folder.setDefault(true);
                    return folderRepository.save(folder);
                });

        Folder currentFolder;
        if (folderId != null) {
            currentFolder = folderRepository.findById(folderId)
                    .filter(f -> f.getUser().getId().equals(userId))
                    .orElse(defaultFolder);
        } else {
            currentFolder = defaultFolder;
        }

        List<Task> tasks = taskRepository.findByFolderOrderByIdDesc(currentFolder);
        tasks.sort((t1, t2) -> {
            boolean isDone1 = t1.getStatus() == Task.TaskStatus.COMPLETED;
            boolean isDone2 = t2.getStatus() == Task.TaskStatus.COMPLETED;
            if (isDone1 && !isDone2) return 1;
            if (!isDone1 && isDone2) return -1;
            return t2.getId().compareTo(t1.getId());
        });
        
        List<Folder> folders = folderRepository.findByUserOrderByIsDefaultDescIdAsc(user);
        
        model.addAttribute("tasks", tasks);
        model.addAttribute("folders", folders);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("newTask", new Task());
        model.addAttribute("statuses", Task.TaskStatus.values());

        return "index";
    }

    @PostMapping("/tasks/add")
    public String addTask(@ModelAttribute Task task, 
                         @RequestParam(required = false) Long folderId,
                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Folder folder;
            if (folderId != null) {
                folder = folderRepository.findById(folderId)
                        .filter(f -> f.getUser().getId().equals(userId))
                        .orElseGet(() -> folderRepository.findByUserAndIsDefault(user, true).orElse(null));
            } else {
                folder = folderRepository.findByUserAndIsDefault(user, true).orElse(null);
            }
            
            task.setUser(user);
            task.setFolder(folder);
            if (task.getStatus() == null) {
                task.setStatus(Task.TaskStatus.NOT_STARTED);
            }
            taskRepository.save(task);
        }

        return folderId != null ? "redirect:/?folderId=" + folderId : "redirect:/";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(@PathVariable Long id, @RequestParam String status, 
                                   @RequestParam(required = false) Long folderId,
                                   HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Task task = taskRepository.findById(id).orElse(null);
        if (task != null && task.getUser().getId().equals(userId)) {
            task.setStatus(Task.TaskStatus.valueOf(status));
            taskRepository.save(task);
        }

        return folderId != null ? "redirect:/?folderId=" + folderId : "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, 
                            @RequestParam(required = false) Long folderId,
                            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Task task = taskRepository.findById(id).orElse(null);
        if (task != null && task.getUser().getId().equals(userId)) {
            taskRepository.delete(task);
        }

        return folderId != null ? "redirect:/?folderId=" + folderId : "redirect:/";
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
                                 @RequestParam(required = false) Long folderId,
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
            folderId = task.getFolder().getId();
        }
        return folderId != null ? "redirect:/?folderId=" + folderId : "redirect:/";
    }
}
