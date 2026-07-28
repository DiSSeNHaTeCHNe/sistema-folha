package br.com.techne.sistemafolha.folha.domain;

public class FichaMensalNotFoundException extends RuntimeException {

    public FichaMensalNotFoundException(Long id) {
        super("Ficha mensal não encontrada: " + id);
    }
}
