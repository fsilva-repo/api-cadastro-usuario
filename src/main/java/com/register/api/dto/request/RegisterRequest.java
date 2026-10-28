package com.register.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
 @NotBlank String name,
 @NotBlank String email,
 @NotBlank @Size(min =  8) String password
) {}

