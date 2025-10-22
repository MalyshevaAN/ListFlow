package com.example.xp.controllers;

import com.example.xp.entity.Folder;
import com.example.xp.entity.User;
import com.example.xp.repository.FolderRepository;
import com.example.xp.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/folders")
public class FolderController {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add")
    public String addFolder(@RequestParam String name, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Folder folder = new Folder();
            folder.setName(name);
            folder.setUser(user);
            folder.setDefault(false);
            folderRepository.save(folder);
        }

        return "redirect:/";
    }

    @PostMapping("/{id}/rename")
    public String renameFolder(@PathVariable Long id, @RequestParam String name, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Folder> folderOpt = folderRepository.findById(id);
        if (folderOpt.isPresent() && folderOpt.get().getUser().getId().equals(userId) && !folderOpt.get().isDefault()) {
            Folder folder = folderOpt.get();
            folder.setName(name);
            folderRepository.save(folder);
        }

        return "redirect:/";
    }

    @PostMapping("/{id}/delete")
    public String deleteFolder(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Optional<Folder> folderOpt = folderRepository.findById(id);
        if (folderOpt.isPresent() && folderOpt.get().getUser().getId().equals(userId) && !folderOpt.get().isDefault()) {
            folderRepository.delete(folderOpt.get());
        }

        return "redirect:/";
    }
}
