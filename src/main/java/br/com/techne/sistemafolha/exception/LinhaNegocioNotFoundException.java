package br.com.techne.sistemafolha.exception;

public class LinhaNegocioNotFoundException extends RuntimeException {
    public LinhaNegocioNotFoundException(Long id) {
        super("Linha de negócio não encontrada com ID: " + id);
    }
} 