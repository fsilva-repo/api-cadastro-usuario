package com.register.api.dto.response;

public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn) {
}