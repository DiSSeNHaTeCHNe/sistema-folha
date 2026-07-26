package br.com.techne.sistemafolha.exception;

public class BeneficioMensalDuplicadaException extends RuntimeException {

    private final String competenciaInicio;
    private final String competenciaFim;

    public BeneficioMensalDuplicadaException(String message, String competenciaInicio, String competenciaFim) {
        super(message);
        this.competenciaInicio = competenciaInicio;
        this.competenciaFim = competenciaFim;
    }

    public String getCompetenciaInicio() {
        return competenciaInicio;
    }

    public String getCompetenciaFim() {
        return competenciaFim;
    }
}
