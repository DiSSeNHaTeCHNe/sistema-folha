package br.com.techne.sistemafolha.beneficios.domain;

public class TipoBeneficioCodigoDuplicadoException extends RuntimeException {
    public TipoBeneficioCodigoDuplicadoException(String codigo) {
        super("Já existe um tipo de benefício com o código: " + codigo);
    }
}
