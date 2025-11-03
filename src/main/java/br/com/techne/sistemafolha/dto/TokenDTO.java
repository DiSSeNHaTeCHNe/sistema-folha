package br.com.techne.sistemafolha.dto;

import java.time.LocalDateTime;

public record TokenDTO(
    String login,
    String token,
    String refreshToken,
    LocalDateTime tokenExpiration,
    LocalDateTime refreshExpiration,
    AcessoUsuarioDTO acessoUsuario
) {
    // Construtor para compatibilidade com código existente
    public TokenDTO(String login, String token) {
        this(login, token, null, null, null, null);
    }
    
    // Construtor sem informações de acesso
    public TokenDTO(String login, String token, String refreshToken, 
                    LocalDateTime tokenExpiration, LocalDateTime refreshExpiration) {
        this(login, token, refreshToken, tokenExpiration, refreshExpiration, null);
    }
} 