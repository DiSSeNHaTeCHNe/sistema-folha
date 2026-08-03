package br.com.techne.sistemafolha.relatorios.domain;

public class RelatorioNotFoundException extends RuntimeException {

    public RelatorioNotFoundException(Long id) {
        super("Relatório não encontrado com ID: " + id);
    }
}
