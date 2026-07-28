package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaTotalizacaoService {

    private final BeneficioConsultaPort beneficioConsultaPort;

    @Transactional(readOnly = true)
    public List<FolhaTotaisFuncionarioDTO> calcularTotaisPorFuncionario(List<FolhaPagamento> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            return List.of();
        }

        Map<Long, List<FolhaPagamento>> porFuncionario = linhas.stream()
            .collect(Collectors.groupingBy(l -> l.getFuncionario().getId()));

        List<FolhaTotaisFuncionarioDTO> resultado = new ArrayList<>();

        for (List<FolhaPagamento> grupo : porFuncionario.values()) {
            FolhaPagamento referencia = grupo.get(0);
            Funcionario funcionario = referencia.getFuncionario();
            LocalDate competenciaInicio = grupo.stream()
                .map(FolhaPagamento::getDataInicio)
                .min(LocalDate::compareTo)
                .orElse(referencia.getDataInicio());
            LocalDate competenciaFim = grupo.stream()
                .map(FolhaPagamento::getDataFim)
                .max(LocalDate::compareTo)
                .orElse(referencia.getDataFim());

            List<FolhaMotorCalculo.LinhaCalculoInput> inputs = grupo.stream()
                .map(this::toInput)
                .toList();
            FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(inputs);

            int totalBeneficios = beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                funcionario.getId(), competenciaInicio, competenciaFim);
            BigDecimal custoBeneficios = beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                funcionario.getId(), competenciaInicio, competenciaFim);

            BigDecimal salBruto = totais.bruto();
            BigDecimal salLiquido = totais.liquido();
            BigDecimal salCustoFolha = totais.custoFolha();
            BigDecimal salCustoBeneficios = FolhaMotorCalculo.arredondar(
                custoBeneficios != null ? custoBeneficios : BigDecimal.ZERO);
            BigDecimal salCustoTechne = FolhaCustoEmpresaComposer.compor(
                salCustoFolha, BigDecimal.ZERO, salCustoBeneficios);

            Cargo cargo = referencia.getCargo() != null ? referencia.getCargo() : funcionario.getCargo();
            CentroCusto centroCusto = referencia.getCentroCusto() != null
                ? referencia.getCentroCusto()
                : funcionario.getCentroCusto();
            LinhaNegocio linhaNegocio = referencia.getLinhaNegocio();

            resultado.add(new FolhaTotaisFuncionarioDTO(
                funcionario.getId(),
                funcionario.getNome(),
                competenciaInicio,
                competenciaFim,
                cargo != null ? cargo.getId() : null,
                cargo != null ? cargo.getDescricao() : null,
                centroCusto != null ? centroCusto.getId() : null,
                centroCusto != null ? centroCusto.getDescricao() : null,
                linhaNegocio != null ? linhaNegocio.getId() : null,
                linhaNegocio != null ? linhaNegocio.getDescricao() : null,
                grupo.size(),
                totalBeneficios,
                salBruto,
                salLiquido,
                salCustoFolha,
                salCustoBeneficios,
                salCustoTechne
            ));
        }

        resultado.sort((a, b) -> a.funcionarioNome().compareToIgnoreCase(b.funcionarioNome()));
        return resultado;
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FolhaPagamento linha) {
        Rubrica rubrica = linha.getRubrica();
        BigDecimal valor = linha.getValor() != null ? linha.getValor() : BigDecimal.ZERO;
        if (rubrica == null) {
            return new FolhaMotorCalculo.LinhaCalculoInput(valor, (short) 0, (short) 0, (short) 0);
        }
        return new FolhaMotorCalculo.LinhaCalculoInput(
            valor,
            operadorOuZero(rubrica.getOperadorBruto()),
            operadorOuZero(rubrica.getOperadorLiquido()),
            operadorOuZero(rubrica.getOperadorCusto())
        );
    }

    private short operadorOuZero(Short operador) {
        return operador != null ? operador : 0;
    }
}
