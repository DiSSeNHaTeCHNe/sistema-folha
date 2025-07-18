package br.com.techne.sistemafolha.exception;

public class OrganogramaAtivoConflictException extends RuntimeException {
    
    public OrganogramaAtivoConflictException() {
        super("Já existe um organograma ativo. Apenas um organograma pode estar ativo por vez.");
    }
    
    public OrganogramaAtivoConflictException(String message) {
        super(message);
    }
} 