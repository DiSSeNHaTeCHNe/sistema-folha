package br.com.techne.sistemafolha.auth.domain;

public class UsuarioNotFoundException extends RuntimeException {
    public UsuarioNotFoundException(Long id) {
        super("Usuário não encontrado com ID: " + id);
    }

    public UsuarioNotFoundException(String login) {
        super("Usuário não encontrado com login: " + login);
    }
} 