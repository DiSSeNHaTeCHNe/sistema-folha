package br.com.techne.sistemafolha.relatorios.domain;

public class RelatorioGeracaoLimiteException extends RuntimeException {

    public RelatorioGeracaoLimiteException(int limite) {
        super("Limite de " + limite + " gerações simultâneas por usuário atingido");
    }
}
