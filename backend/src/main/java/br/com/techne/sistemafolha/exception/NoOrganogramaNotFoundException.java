package br.com.techne.sistemafolha.exception;

public class NoOrganogramaNotFoundException extends RuntimeException {
    
    public NoOrganogramaNotFoundException(Long id) {
        super("Nó do organograma não encontrado com ID: " + id);
    }
    
    public NoOrganogramaNotFoundException(String message) {
        super(message);
    }
} 