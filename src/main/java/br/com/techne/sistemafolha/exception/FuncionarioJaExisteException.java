package br.com.techne.sistemafolha.exception;
 
public class FuncionarioJaExisteException extends RuntimeException {
    public FuncionarioJaExisteException(String message) {
        super(message);
    }
} 