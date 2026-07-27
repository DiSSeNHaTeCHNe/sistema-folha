package br.com.techne.sistemafolha.beneficios.domain;

public class BeneficioMensalNotFoundException extends RuntimeException {
    public BeneficioMensalNotFoundException(Long id) {
        super("Benefício mensal não encontrado com ID: " + id);
    }
}
