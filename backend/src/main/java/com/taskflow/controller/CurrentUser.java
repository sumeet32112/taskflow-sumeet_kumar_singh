package com.taskflow.controller;

import com.taskflow.repository.UserRepository;
import com.taskflow.exception.NotFoundException;
import com.taskflow.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves the currently authenticated user from the SecurityContext.
 * Controllers call this instead of coupling directly to the JWT infrastructure.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional(readOnly = true)
    public User user() {
        return userRepository.findByEmail(email())
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public UUID id() {
        return user().getId();
    }
}
