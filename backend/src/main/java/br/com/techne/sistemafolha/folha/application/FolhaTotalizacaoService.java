package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaTotalizacaoService {

    private final BeneficioConsultaPort beneficioConsultaPort;

    @Transactional(readOnly = true)
    public List<FolhaTotaisFuncionarioDTO> calcularTotaisPorFuncionario(
            List<FolhaLinhaSnapshot> linhas,
            AccessContextDTO contexto,
            BigDecimal totalEncargosSnapshot,
            LocalDate competenciaInicio,
            LocalDate competenciaFim) {
        if (linhas == null || linhas.isEmpty()) {
            return List.of();
        }

        Map<Long, List<FolhaLinhaSnapshot>> porFuncionario = linhas.stream()
            .filter(l -> l.funcionarioId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::funcionarioId));

        Set<Long> funcionarioIds = porFuncionario.keySet();
        Map<Long, BigDecimal> beneficiosPorFuncionario = beneficioConsultaPort
            .somarValorPorFuncionariosECompetencia(funcionarioIds, competenciaInicio, competenciaFim);

        List<FolhaTotaisFuncionarioDTO> resultado = new ArrayList<>();

        for (Map.Entry<Long, List<FolhaLinhaSnapshot>> entry : porFuncionario.entrySet()) {
            Long funcionarioId = entry.getKey();
            List<FolhaLinhaSnapshot> grupo = entry.getValue();
            FolhaLinhaSnapshot referencia = grupo.get(0);

            List<FolhaMotorCalculo.LinhaCalculoInput> inputs = grupo.stream()
                .map(this::toInput)
                .toList();
            FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(inputs);

            int totalBeneficios = beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                funcionarioId, competenciaInicio, competenciaFim);
            BigDecimal custoBeneficios = FolhaMotorCalculo.arredondar(
                beneficiosPorFuncionario.getOrDefault(funcionarioId, BigDecimal.ZERO));
            BigDecimal encargosRateados = BigDecimal.ZERO.setScale(2);
            BigDecimal custoEmpresa = FolhaCustoEmpresaComposer.compor(
                totais.custoFolha(), encargosRateados, custoBeneficios);

            resultado.add(new FolhaTotaisFuncionarioDTO(
                funcionarioId,
                referencia.funcionarioNome(),
                competenciaInicio,
                competenciaFim,
                referencia.cargoId(),
                referencia.cargoDescricao(),
                referencia.centroCustoId(),
                referencia.centroCustoDescricao(),
                referencia.linhaNegocioId(),
                referencia.linhaNegocioDescricao(),
                grupo.size(),
                totalBeneficios,
                totais.bruto(),
                totais.liquido(),
                totais.custoFolha(),
                custoBeneficios,
                encargosRateados,
                custoEmpresa
            ));
        }

        resultado.sort((a, b) -> a.funcionarioNome().compareToIgnoreCase(b.funcionarioNome()));
        return resultado;
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FolhaLinhaSnapshot linha) {
        BigDecimal valor = linha.valor() != null ? linha.valor() : BigDecimal.ZERO;
        return new FolhaMotorCalculo.LinhaCalculoInput(
            valor, linha.operadorBruto(), linha.operadorLiquido(), linha.operadorCusto(), linha.porcentagem());
    }
}
