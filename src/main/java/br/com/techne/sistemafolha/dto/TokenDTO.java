package br.com.techne.sistemafolha.dto;

import java.time.LocalDateTime;

public record TokenDTO(
    String login,
    String token,
    String refreshToken,
    LocalDateTime tokenExpiration,
    LocalDateTime refreshExpiration
) {
    // Construtor para compatibilidade com código existente
    public TokenDTO(String login, String token) {
        this(login, token, null, null, null);
    }
} 