package com.register.api.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.register.api.dto.response.AuthTokenResponse;
import com.register.api.model.AuthProvider;
import com.register.api.model.User;
import com.register.api.repository.UserRepository;
import com.register.api.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final JwtService jwtService;

    public OAuth2SuccessHandler(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String googleId = principal.getAttribute("sub");
        Boolean emailVerified = principal.getAttribute("email_verified");

        // A sessão só serve para o fluxo de redirecionamento ao Google.
        // Daqui em diante a API autentica apenas por JWT.
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (email == null || googleId == null || !Boolean.TRUE.equals(emailVerified)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "E-mail do Google ausente ou não verificado");
            return;
        }

        User user = users.findByProviderAndProviderId(AuthProvider.GOOGLE, googleId).orElse(null);

        if (user == null) {
            if (users.existsByEmail(email)) {
                response.sendError(HttpServletResponse.SC_CONFLICT,
                        "Já existe uma conta com este e-mail. Entre com e-mail e senha.");
                return;
            }
            user = users.save(new User(
                    name != null ? name : email,
                    email,
                    null,
                    AuthProvider.GOOGLE,
                    googleId,
                    Set.of("USER")));
        }

        AuthTokenResponse token = jwtService.generate(user);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"accessToken\":\"" + token.accessToken() + "\","
                + "\"tokenType\":\"" + token.tokenType() + "\","
                + "\"expiresIn\":" + token.expiresIn() + "}");
    }
}