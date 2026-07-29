package br.com.techne.sistemafolha.folha.port;

import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FolhaTotalizacaoPort {

    /**
     * Agrega custo empresa (custo folha + benefícios, sem encargos rateados) para as linhas informadas.
     */
    BigDecimal calcularTotalCustoEmpresa(
        List<FolhaLinhaSnapshot> linhas,
        LocalDate competenciaInicio,
        LocalDate competenciaFim,
        AccessContextDTO contexto);
}
