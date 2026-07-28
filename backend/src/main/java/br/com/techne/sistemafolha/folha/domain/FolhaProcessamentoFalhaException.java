package br.com.techne.sistemafolha.folha.domain;

public class FolhaProcessamentoFalhaException extends RuntimeException {

    public FolhaProcessamentoFalhaException(String message) {
        super(message);
    }

    public FolhaProcessamentoFalhaException(String message, Throwable cause) {
        super(message, cause);
    }
}
