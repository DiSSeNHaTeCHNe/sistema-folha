package br.com.techne.sistemafolha.exception;

public class FolhaDuplicadaException extends RuntimeException {
    
    private final String competenciaInicio;
    private final String competenciaFim;
    private final boolean decimoTerceiro;
    
    public FolhaDuplicadaException(String message, String competenciaInicio, String competenciaFim, boolean decimoTerceiro) {
        super(message);
        this.competenciaInicio = competenciaInicio;
        this.competenciaFim = competenciaFim;
        this.decimoTerceiro = decimoTerceiro;
    }
    
    public String getCompetenciaInicio() {
        return competenciaInicio;
    }
    
    public String getCompetenciaFim() {
        return competenciaFim;
    }
    
    public boolean isDecimoTerceiro() {
        return decimoTerceiro;
    }
}
