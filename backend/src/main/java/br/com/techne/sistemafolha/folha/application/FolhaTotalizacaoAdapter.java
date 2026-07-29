package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolhaTotalizacaoAdapter implements FolhaTotalizacaoPort {

    private final FolhaTotalizacaoService folhaTotalizacaoService;

    @Override
    public BigDecimal calcularTotalCustoEmpresa(
            List<FolhaLinhaSnapshot> linhas,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            AccessContextDTO contexto) {
        return folhaTotalizacaoService.calcularTotalCustoEmpresa(
            linhas, competenciaInicio, competenciaFim, contexto);
    }
}
