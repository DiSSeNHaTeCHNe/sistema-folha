package br.com.techne.sistemafolha.workspace.port;

import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface OrcamentoConsultaPort {

    List<OrcamentoCentroCustoDTO> obterRealizadoPorCentroCusto(AccessContextDTO ctx, YearMonth competencia);

    List<OrcamentoNodeDTO> consolidarHierarquia(AccessContextDTO ctx, YearMonth competencia);

    record OrcamentoCentroCustoDTO(
        Long centroCustoId,
        String centroCustoDescricao,
        BigDecimal realizado,
        long quantidadeFuncionarios
    ) {}

    record OrcamentoNodeDTO(
        Long noId,
        String noNome,
        BigDecimal realizado,
        List<OrcamentoNodeDTO> filhos
    ) {}
}
