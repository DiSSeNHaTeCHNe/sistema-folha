package br.com.techne.sistemafolha.relatorios.domain;

public class RelatorioIndisponivelException extends RuntimeException {

    public RelatorioIndisponivelException(RelatorioStatus status) {
        super("Relatório indisponível para download (status: " + status + ")");
    }
}
