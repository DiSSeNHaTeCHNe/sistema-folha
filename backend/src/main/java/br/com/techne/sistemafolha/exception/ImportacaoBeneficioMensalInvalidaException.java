package br.com.techne.sistemafolha.exception;

import java.util.List;

public class ImportacaoBeneficioMensalInvalidaException extends RuntimeException {

    private final List<String> detalhesErros;

    public ImportacaoBeneficioMensalInvalidaException(List<String> detalhesErros) {
        super(formatarMensagem(detalhesErros));
        this.detalhesErros = List.copyOf(detalhesErros);
    }

    public List<String> getDetalhesErros() {
        return detalhesErros;
    }

    private static String formatarMensagem(List<String> detalhesErros) {
        if (detalhesErros == null || detalhesErros.isEmpty()) {
            return "Importação rejeitada: arquivo contém erros de validação";
        }
        return "Importação rejeitada. Nenhum registro foi salvo. Erros encontrados: "
                + String.join("; ", detalhesErros);
    }
}
