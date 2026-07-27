package br.com.techne.sistemafolha.cadastros.domain;

public class CentroCustoNotFoundException extends RuntimeException {
    public CentroCustoNotFoundException(Long id) {
        super("Centro de custo não encontrado com ID: " + id);
    }
} 