package br.com.techne.sistemafolha.dashboard.domain;

public class DashboardAcessoNegadoException extends RuntimeException {

    public DashboardAcessoNegadoException() {
        super("Acesso negado ao dashboard customizável");
    }
}
