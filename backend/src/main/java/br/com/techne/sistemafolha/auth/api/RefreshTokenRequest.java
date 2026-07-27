package br.com.techne.sistemafolha.auth.api;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token é obrigatório")
    String refreshToken
) {} 