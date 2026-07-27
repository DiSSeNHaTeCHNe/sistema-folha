package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaTotalizacaoService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

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

            BigDecimal bruto = BigDecimal.ZERO;
            BigDecimal liquido = BigDecimal.ZERO;
            BigDecimal custoFolha = BigDecimal.ZERO;

            for (FolhaPagamento linha : grupo) {
                BigDecimal valor = linha.getValor() != null ? linha.getValor() : BigDecimal.ZERO;
                CoeficientesRubrica coef = coeficientesDe(linha.getRubrica());
                bruto = bruto.add(valor.multiply(coef.bruto()));
                liquido = liquido.add(valor.multiply(coef.liquido()));
                custoFolha = custoFolha.add(valor.multiply(coef.custo()));
            }

            int totalBeneficios = beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                funcionario.getId(), competenciaInicio, competenciaFim);
            BigDecimal custoBeneficios = beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                funcionario.getId(), competenciaInicio, competenciaFim);

            BigDecimal salBruto = arredondar(bruto);
            BigDecimal salLiquido = arredondar(liquido);
            BigDecimal salCustoFolha = arredondar(custoFolha);
            BigDecimal salCustoBeneficios = arredondar(custoBeneficios);
            BigDecimal salCustoTechne = salCustoFolha.add(salCustoBeneficios);

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

    private CoeficientesRubrica coeficientesDe(Rubrica rubrica) {
        if (rubrica == null || rubrica.getTipoRubrica() == null) {
            return new CoeficientesRubrica(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        String tipo = rubrica.getTipoRubrica().getDescricao();
        if (tipo == null) {
            return new CoeficientesRubrica(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return switch (tipo.toUpperCase()) {
            case "PROVENTO" -> new CoeficientesRubrica(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
            case "DESCONTO" -> new CoeficientesRubrica(BigDecimal.ZERO, BigDecimal.ONE.negate(), BigDecimal.ZERO);
            case "INFORMATIVO" -> new CoeficientesRubrica(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            default -> new CoeficientesRubrica(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        };
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(SCALE, ROUNDING);
    }

    private record CoeficientesRubrica(BigDecimal bruto, BigDecimal liquido, BigDecimal custo) {}
}
