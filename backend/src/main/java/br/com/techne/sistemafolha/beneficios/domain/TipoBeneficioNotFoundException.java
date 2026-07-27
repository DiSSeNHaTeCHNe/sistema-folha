package br.com.techne.sistemafolha.beneficios.domain;

public class TipoBeneficioNotFoundException extends RuntimeException {
    public TipoBeneficioNotFoundException(Long id) {
        super("Tipo de benefício não encontrado com ID: " + id);
    }
}
