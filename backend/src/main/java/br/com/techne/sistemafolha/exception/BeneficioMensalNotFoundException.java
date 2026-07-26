package br.com.techne.sistemafolha.exception;

public class BeneficioMensalNotFoundException extends RuntimeException {
    public BeneficioMensalNotFoundException(Long id) {
        super("Benefício mensal não encontrado com ID: " + id);
    }
}
