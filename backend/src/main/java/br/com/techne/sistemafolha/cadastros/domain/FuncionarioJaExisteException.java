package br.com.techne.sistemafolha.cadastros.domain;
 
public class FuncionarioJaExisteException extends RuntimeException {
    public FuncionarioJaExisteException(String message) {
        super(message);
    }
} 