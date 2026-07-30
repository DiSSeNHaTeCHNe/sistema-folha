package br.com.techne.sistemafolha.auth.domain;

public class RefreshTokenInvalidoException extends RuntimeException {
    public RefreshTokenInvalidoException(String message) {
        super(message);
    }
}
