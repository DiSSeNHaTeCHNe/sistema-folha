package br.com.techne.sistemafolha.cadastros.domain;

public class FuncionarioNotFoundException extends RuntimeException {
    public FuncionarioNotFoundException(Long id) {
        super("Funcionário não encontrado com ID: " + id);
    }
} 