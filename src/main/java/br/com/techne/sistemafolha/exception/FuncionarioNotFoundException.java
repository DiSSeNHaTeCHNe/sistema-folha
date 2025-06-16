package br.com.techne.sistemafolha.exception;

public class FuncionarioNotFoundException extends RuntimeException {
    public FuncionarioNotFoundException(Long id) {
        super("Funcionário não encontrado com ID: " + id);
    }
} 