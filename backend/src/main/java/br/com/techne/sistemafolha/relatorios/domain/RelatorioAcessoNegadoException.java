package br.com.techne.sistemafolha.relatorios.domain;

public class RelatorioAcessoNegadoException extends RuntimeException {

    public RelatorioAcessoNegadoException() {
        super("Acesso negado para gerar relatório na competência informada");
    }
}
