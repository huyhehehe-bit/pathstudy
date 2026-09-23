package com.pathstudy.web;

import com.pathstudy.domain.User;
import com.pathstudy.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (users.existsByEmail(form.getEmail())) {
            binding.rejectValue("email", "exists", "Email này đã được đăng ký");
        }
        if (binding.hasErrors()) {
            return "auth/register";
        }

        User u = new User();
        u.setFullName(form.getFullName().strip());
        u.setEmail(form.getEmail().strip().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        u.setGrade(form.getGrade());
        users.save(u);

        autoLogin(u.getEmail(), form.getPassword(), request, response);
        return "redirect:/subjects";
    }

    private void autoLogin(String email, String rawPassword,
                           HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
