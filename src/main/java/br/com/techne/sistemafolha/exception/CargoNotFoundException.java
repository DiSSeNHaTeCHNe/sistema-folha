package br.com.techne.sistemafolha.exception;

public class CargoNotFoundException extends RuntimeException {
    public CargoNotFoundException(Long id) {
        super("Cargo não encontrado com ID: " + id);
    }
} 