package br.com.techne.sistemafolha.auth.domain;

public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(Long id) {
        super("API Key não encontrada com ID: " + id);
    }

    public ApiKeyNotFoundException() {
        super("API Key não encontrada");
    }
}
