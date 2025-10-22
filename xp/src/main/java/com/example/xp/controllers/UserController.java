package com.example.xp.controllers;

import com.example.xp.entity.User;
import com.example.xp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute User user, Model model) {
        if (userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Пользователь с таким email уже существует!");
            return "register";
        }

        user.setPassword(user.getPassword());
        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        session.invalidate();
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute User user, Model model, HttpSession session) {
        Optional<User> existing = userRepository.findByEmailAndPassword(user.getEmail(), user.getPassword());

        if (existing.isPresent()) {
            session.setAttribute("userId", existing.get().getId());
            return "redirect:/";
        } else {
            model.addAttribute("error", "Неверный email или пароль!");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
