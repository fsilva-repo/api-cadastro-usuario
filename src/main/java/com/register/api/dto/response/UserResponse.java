package com.register.api.dto.response;

import com.register.api.model.AuthProvider;

public record UserResponse(
  String id, String name, String email, AuthProvider provider
) {}
