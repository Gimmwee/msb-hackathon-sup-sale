package com.msb.supsale.controller;

import com.msb.supsale.dto.CreateUserRequest;
import com.msb.supsale.dto.UserDto;
import com.msb.supsale.model.User;
import com.msb.supsale.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userRepository.findAll().stream().map(UserDto::new).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.Role.valueOf(request.getRole()));
        user.setActive(true);
        return new UserDto(userRepository.save(user));
    }

    @PatchMapping("/{id}")
    public UserDto updateUser(@PathVariable UUID id, @RequestBody Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        if (updates.containsKey("active")) {
            boolean newActive = (boolean) updates.get("active");
            if (!newActive && user.getUsername().equals(currentUsername)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot lock your own account");
            }
            user.setActive(newActive);
        }
        if (updates.containsKey("role")) {
            user.setRole(User.Role.valueOf((String) updates.get("role")));
        }
        return new UserDto(userRepository.save(user));
    }
}
