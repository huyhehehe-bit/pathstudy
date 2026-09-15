package com.pathstudy.service;

import com.pathstudy.domain.User;
import com.pathstudy.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return Optional.empty();
        }
        return users.findByEmail(name);
    }

    public User require() {
        return current().orElseThrow(() -> new IllegalStateException("Chưa đăng nhập"));
    }
}
