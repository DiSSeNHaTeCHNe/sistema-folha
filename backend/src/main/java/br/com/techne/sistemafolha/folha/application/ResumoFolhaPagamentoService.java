package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumoFolhaPagamentoService {

    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;
    private final FolhaConsultaPort folhaConsultaPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final UsuarioLookupPort usuarioLookupPort;
    private final BeneficioConsultaPort beneficioConsultaPort;
    private final EncargosRateioService encargosRateioService;
    private final FichaMensalRepository fichaMensalRepository;
    private final FolhaLinhaAgregacao folhaLinhaAgregacao = new FolhaLinhaAgregacao();

    public List<ResumoFolhaPagamentoDTO> listarTodos(String login, Integer ano, Integer mes) {
        PeriodoCompetencia periodo = periodoDe(ano, mes);
        return consultarPorPeriodo(login, periodo.inicio(), periodo.fim());
    }

    private record PeriodoCompetencia(LocalDate inicio, LocalDate fim) {}

    private PeriodoCompetencia periodoDe(Integer ano, Integer mes) {
        int anoEfetivo = ano != null ? ano : LocalDate.now().getYear();
        if (anoEfetivo < 2000 || anoEfetivo > 2100) {
            throw new IllegalArgumentException("Ano deve estar entre 2000 e 2100");
        }
        if (mes != null) {
            LocalDate inicio = LocalDate.of(anoEfetivo, mes, 1);
            LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
            return new PeriodoCompetencia(inicio, fim);
        }
        return new PeriodoCompetencia(
            LocalDate.of(anoEfetivo, 1, 1),
            LocalDate.of(anoEfetivo, 12, 31)
        );
    }

    public List<ResumoFolhaPagamentoDTO> consultarPorPeriodo(
            String login, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Collections.emptyList();
        }
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioBetweenAndAtivoTrue(dataInicio, dataFim)
            .stream()
            .map(resumo -> mapear(resumo, contexto))
            .collect(Collectors.toList());
    }

    public Optional<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            String login, LocalDate competenciaInicio, LocalDate competenciaFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Optional.empty();
        }
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .map(resumo -> mapear(resumo, contexto));
    }

    public List<ResumoFolhaPagamentoDTO> listarMaisRecentes(String login) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Collections.emptyList();
        }
        return resumoFolhaPagamentoRepository.findLatestResumos()
            .stream()
            .map(resumo -> mapear(resumo, contexto))
            .collect(Collectors.toList());
    }

    private ResumoFolhaPagamentoDTO mapear(ResumoFolhaPagamento resumo, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return toDtoGlobal(resumo);
        }
        return toDtoScoped(resumo, contexto.centrosCustoIds());
    }

    private ResumoFolhaPagamentoDTO toDtoGlobal(ResumoFolhaPagamento resumo) {
        boolean decimoTerceiro = Boolean.TRUE.equals(resumo.getDecimoTerceiro());

        if (fichaMensalRepository.existsByCompetencia(
                resumo.getCompetenciaInicio(), resumo.getCompetenciaFim(), decimoTerceiro)) {
            return toDtoGlobalFromFicha(resumo, decimoTerceiro);
        }

        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            resumo.getCompetenciaInicio(), resumo.getCompetenciaFim(), decimoTerceiro, null);
        if (!linhas.isEmpty()) {
            return toDtoFromLinhas(resumo, linhas, resumo.getTotalEncargos());
        }

        BigDecimal totalCustoEmpresa = FolhaCustoEmpresaComposer.compor(
            resumo.getTotalPagamentos(), resumo.getTotalEncargos(), BigDecimal.ZERO);
        return montarDto(
            resumo,
            resumo.getTotalEmpregados(),
            resumo.getTotalEncargos(),
            resumo.getTotalPagamentos(),
            resumo.getTotalDescontos(),
            resumo.getTotalLiquido(),
            resumo.getTotalPagamentos(),
            totalCustoEmpresa);
    }

    private ResumoFolhaPagamentoDTO toDtoGlobalFromFicha(ResumoFolhaPagamento resumo, boolean decimoTerceiro) {
        List<FichaMensal> fichas = fichaMensalRepository.findByCompetencia(
            resumo.getCompetenciaInicio(), resumo.getCompetenciaFim(), decimoTerceiro);

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;
        BigDecimal totalCustoFolha = BigDecimal.ZERO;
        Map<Long, BigDecimal> brutoPorFuncionario = new HashMap<>();

        for (FichaMensal ficha : fichas) {
            totalBruto = totalBruto.add(nullSafe(ficha.getBruto()));
            totalLiquido = totalLiquido.add(nullSafe(ficha.getLiquido()));
            totalCustoFolha = totalCustoFolha.add(nullSafe(ficha.getCustoFolha()));
            if (ficha.getFuncionario() != null) {
                brutoPorFuncionario.put(ficha.getFuncionario().getId(), nullSafe(ficha.getBruto()));
            }
        }

        Set<Long> funcionarioIds = brutoPorFuncionario.keySet();
        Map<Long, BigDecimal> beneficios = beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            funcionarioIds, resumo.getCompetenciaInicio(), resumo.getCompetenciaFim());
        Map<Long, BigDecimal> encargosRateados = encargosRateioService.ratearPorFuncionario(
            brutoPorFuncionario, resumo.getTotalEncargos());

        BigDecimal totalBeneficios = somarValores(beneficios);
        BigDecimal totalEncargosRateados = somarValores(encargosRateados);
        BigDecimal totalCustoEmpresa = FolhaCustoEmpresaComposer.compor(
            totalCustoFolha, totalEncargosRateados, totalBeneficios);

        return montarDto(
            resumo,
            fichas.size(),
            resumo.getTotalEncargos(),
            FolhaMotorCalculo.arredondar(totalBruto),
            BigDecimal.ZERO,
            FolhaMotorCalculo.arredondar(totalLiquido),
            FolhaMotorCalculo.arredondar(totalBruto),
            FolhaMotorCalculo.arredondar(totalCustoEmpresa));
    }

    private ResumoFolhaPagamentoDTO toDtoScoped(ResumoFolhaPagamento resumo, Set<Long> centros) {
        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            resumo.getCompetenciaInicio(), resumo.getCompetenciaFim(),
            Boolean.TRUE.equals(resumo.getDecimoTerceiro()), centros);
        return toDtoFromLinhas(resumo, linhas, BigDecimal.ZERO);
    }

    private ResumoFolhaPagamentoDTO toDtoFromLinhas(
            ResumoFolhaPagamento resumo, List<FolhaLinhaSnapshot> linhas, BigDecimal totalEncargosSnapshot) {
        Set<Long> funcionarioIds = linhas.stream()
            .map(FolhaLinhaSnapshot::funcionarioId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, BigDecimal> beneficios = beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            funcionarioIds, resumo.getCompetenciaInicio(), resumo.getCompetenciaFim());

        Map<Long, BigDecimal> brutoPorFuncionario = brutoPorFuncionario(linhas);
        Map<Long, BigDecimal> encargosPorFuncionario = encargosRateioService.ratearPorFuncionario(
            brutoPorFuncionario, totalEncargosSnapshot);

        FolhaLinhaAgregacao.TotaisResumo totais = folhaLinhaAgregacao.agregar(
            linhas, beneficios, encargosPorFuncionario);

        BigDecimal pagamentos = calcularPagamentosLegacy(linhas);
        BigDecimal descontos = calcularDescontosLegacy(linhas);

        return montarDto(
            resumo,
            Math.toIntExact(totais.empregados()),
            totais.totalEncargos(),
            pagamentos,
            descontos,
            totais.totalLiquido(),
            totais.totalBruto(),
            totais.totalCustoEmpresa());
    }

    private ResumoFolhaPagamentoDTO montarDto(
            ResumoFolhaPagamento resumo,
            int totalEmpregados,
            BigDecimal totalEncargos,
            BigDecimal totalPagamentos,
            BigDecimal totalDescontos,
            BigDecimal totalLiquido,
            BigDecimal totalBruto,
            BigDecimal totalCustoEmpresa) {
        return new ResumoFolhaPagamentoDTO(
            resumo.getId(),
            totalEmpregados,
            totalEncargos,
            totalPagamentos,
            totalDescontos,
            totalLiquido,
            totalBruto,
            totalCustoEmpresa,
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getDataImportacao(),
            resumo.getDecimoTerceiro(),
            resumo.getAtivo()
        );
    }

    private Map<Long, BigDecimal> brutoPorFuncionario(List<FolhaLinhaSnapshot> linhas) {
        Map<Long, BigDecimal> bruto = new HashMap<>();
        Map<Long, List<FolhaLinhaSnapshot>> porFuncionario = linhas.stream()
            .filter(l -> l.funcionarioId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::funcionarioId));

        for (Map.Entry<Long, List<FolhaLinhaSnapshot>> entry : porFuncionario.entrySet()) {
            List<FolhaMotorCalculo.LinhaCalculoInput> inputs = entry.getValue().stream()
                .map(l -> new FolhaMotorCalculo.LinhaCalculoInput(
                    l.valor() != null ? l.valor() : BigDecimal.ZERO,
                    l.operadorBruto(), l.operadorLiquido(), l.operadorCusto()))
                .toList();
            bruto.put(entry.getKey(), FolhaMotorCalculo.calcularPorLinhas(inputs).bruto());
        }
        return bruto;
    }

    private BigDecimal calcularPagamentosLegacy(List<FolhaLinhaSnapshot> linhas) {
        return linhas.stream()
            .filter(l -> "PROVENTO".equals(l.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDescontosLegacy(List<FolhaLinhaSnapshot> linhas) {
        return linhas.stream()
            .filter(l -> "DESCONTO".equals(l.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarValores(Map<Long, BigDecimal> valores) {
        return valores.values().stream()
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nullSafe(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private AccessContextDTO obterContextoAcesso(String login) {
        Usuario usuario = usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return organogramaAcessoPort.obterContextoAcesso(usuario.getId());
    }

    private boolean acessoNegado(AccessContextDTO contexto) {
        return !contexto.acessoTotal()
            && (!contexto.temFuncionarioVinculado()
                || !contexto.temNoOrganograma()
                || contexto.centrosCustoIds() == null
                || contexto.centrosCustoIds().isEmpty());
    }
}
