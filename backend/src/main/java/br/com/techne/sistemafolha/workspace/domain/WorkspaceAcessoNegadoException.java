package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceAcessoNegadoException extends RuntimeException {

    public WorkspaceAcessoNegadoException() {
        super("Acesso negado ao workspace do usuário");
    }
}
