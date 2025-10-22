package com.example.xp.controllers;

import com.example.xp.entity.User;
import com.example.xp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


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

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute User user, Model model) {
        Optional<User> existing = userRepository.findByEmail(user.getEmail());

        if (existing.isPresent() && passwordEncoder.matches(user.getPassword(), existing.get().getPassword())) {
            return "redirect:/";
        } else {
            model.addAttribute("error", "Неверный email или пароль!");
            return "login";
        }
    }

    @GetMapping("/")
    public String showHomePage() {
        return "index";
    }
}
