package com.register.api.service;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.register.api.dto.request.LoginRequest;
import com.register.api.dto.request.RegisterRequest;
import com.register.api.dto.response.AuthTokenResponse;
import com.register.api.dto.response.UserResponse;
import com.register.api.model.AuthProvider;
import com.register.api.model.User;
import com.register.api.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        User saved = users.save(new User(
                req.name(),
                req.email(),
                encoder.encode(req.password()),
                AuthProvider.LOCAL,
                null,
                Set.of("USER")));

        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getProvider());
    }

    public AuthTokenResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .filter(u -> u.getProvider() == AuthProvider.LOCAL)
                .filter(u -> u.getPasswordHash() != null)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));

        return jwtService.generate(user);
    }
}