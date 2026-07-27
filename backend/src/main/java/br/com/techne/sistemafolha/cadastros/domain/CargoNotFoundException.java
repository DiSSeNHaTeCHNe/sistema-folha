package br.com.techne.sistemafolha.cadastros.domain;

public class CargoNotFoundException extends RuntimeException {
    public CargoNotFoundException(Long id) {
        super("Cargo não encontrado com ID: " + id);
    }
} 