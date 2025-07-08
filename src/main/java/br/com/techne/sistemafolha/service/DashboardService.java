package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.DashboardStatsDTO;
import br.com.techne.sistemafolha.dto.LinhaNegocioStatsDTO;
import br.com.techne.sistemafolha.dto.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dto.CargoStatsDTO;
import br.com.techne.sistemafolha.dto.RubricaStatsDTO;
import br.com.techne.sistemafolha.dto.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.model.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import br.com.techne.sistemafolha.repository.RubricaRepository;
import br.com.techne.sistemafolha.repository.ResumoFolhaPagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final FuncionarioRepository funcionarioRepository;
    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final BeneficioRepository beneficioRepository;
    private final RubricaRepository rubricaRepository;
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;
    
    public DashboardStatsDTO getStats() {
        // Buscar competência mais recente
        Optional<ResumoFolhaPagamento> resumoMaisRecente = getResumoMaisRecente();
        
        // Se não há resumo, usar dados gerais; senão usar dados da competência específica
        if (resumoMaisRecente.isEmpty()) {
            Long totalFuncionarios = funcionarioRepository.countByAtivoTrue();
            Long totalBeneficiosAtivos = beneficioRepository.countByDataFimIsNullOrDataFimAfter(LocalDate.now());
            
            return new DashboardStatsDTO(
                totalFuncionarios,
                BigDecimal.ZERO,
                totalBeneficiosAtivos,
                List.of(),
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of()
            );
        }
        
        ResumoFolhaPagamento resumo = resumoMaisRecente.get();
        LocalDate competenciaInicio = resumo.getCompetenciaInicio();
        LocalDate competenciaFim = resumo.getCompetenciaFim();
        
        // Buscar dados da folha da competência mais recente
        List<FolhaPagamento> folhaCompetencia = folhaPagamentoRepository.findByCompetenciaAndAtivoTrue(competenciaInicio, competenciaFim);
        
        // Estatísticas gerais da competência específica
        Long totalFuncionarios = (long) folhaCompetencia.stream()
            .map(fp -> fp.getFuncionario().getId())
            .collect(Collectors.toSet())
            .size();
        BigDecimal custoMensalFolha = resumo.getTotalLiquido();
        Long totalBeneficiosAtivos = beneficioRepository.countByDataFimIsNullOrDataFimAfter(LocalDate.now());
        
        // Por linha de negócio
        List<LinhaNegocioStatsDTO> porLinhaNegocio = calcularStatsPorLinhaNegocio(folhaCompetencia);
        
        // Por centro de custo
        List<CentroCustoStatsDTO> porCentroCusto = calcularStatsPorCentroCusto(folhaCompetencia);
        
        // Por cargo
        List<CargoStatsDTO> porCargo = calcularStatsPorCargo(folhaCompetencia);
        
        // Proventos e descontos
        BigDecimal totalProventos = calcularTotalProventos(folhaCompetencia);
        BigDecimal totalDescontos = calcularTotalDescontos(folhaCompetencia);
        List<RubricaStatsDTO> topProventos = calcularTopProventos(folhaCompetencia);
        List<RubricaStatsDTO> topDescontos = calcularTopDescontos(folhaCompetencia);
        
        // Evolução mensal
        List<EvolucaoMensalDTO> evolucaoMensal = calcularEvolucaoMensal();
        
        return new DashboardStatsDTO(
            totalFuncionarios,
            custoMensalFolha,
            totalBeneficiosAtivos,
            porLinhaNegocio,
            porCentroCusto,
            porCargo,
            totalProventos,
            totalDescontos,
            topProventos,
            topDescontos,
            evolucaoMensal
        );
    }
    
    private Optional<ResumoFolhaPagamento> getResumoMaisRecente() {
        List<ResumoFolhaPagamento> resumos = resumoFolhaPagamentoRepository.findByCompetenciaMaisRecente();
        return resumos.isEmpty() ? Optional.empty() : Optional.of(resumos.get(0));
    }
    
    private List<LinhaNegocioStatsDTO> calcularStatsPorLinhaNegocio(List<FolhaPagamento> folhaCompetencia) {
        Map<Long, List<FolhaPagamento>> pagamentosPorLinha = folhaCompetencia.stream()
            .collect(Collectors.groupingBy(fp -> fp.getFuncionario().getCentroCusto().getLinhaNegocio().getId()));
        
        return pagamentosPorLinha.entrySet().stream()
            .map(entry -> {
                Long linhaId = entry.getKey();
                List<FolhaPagamento> pagamentos = entry.getValue();
                
                String descricao = pagamentos.get(0).getFuncionario().getCentroCusto().getLinhaNegocio().getDescricao();
                Long quantidadeFuncionarios = pagamentos.stream()
                    .map(fp -> fp.getFuncionario().getId())
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaPagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new LinhaNegocioStatsDTO(linhaId, descricao, quantidadeFuncionarios, valorTotal);
            })
            .collect(Collectors.toList());
    }
    
    private List<CentroCustoStatsDTO> calcularStatsPorCentroCusto(List<FolhaPagamento> folhaCompetencia) {
        Map<Long, List<FolhaPagamento>> pagamentosPorCentro = folhaCompetencia.stream()
            .collect(Collectors.groupingBy(fp -> fp.getFuncionario().getCentroCusto().getId()));
        
        return pagamentosPorCentro.entrySet().stream()
            .map(entry -> {
                Long centroId = entry.getKey();
                List<FolhaPagamento> pagamentos = entry.getValue();
                
                String descricao = pagamentos.get(0).getFuncionario().getCentroCusto().getDescricao();
                Long quantidadeFuncionarios = pagamentos.stream()
                    .map(fp -> fp.getFuncionario().getId())
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaPagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new CentroCustoStatsDTO(centroId, descricao, quantidadeFuncionarios, valorTotal);
            })
            .collect(Collectors.toList());
    }
    
    private List<CargoStatsDTO> calcularStatsPorCargo(List<FolhaPagamento> folhaCompetencia) {
        Map<Long, List<FolhaPagamento>> pagamentosPorCargo = folhaCompetencia.stream()
            .collect(Collectors.groupingBy(fp -> fp.getFuncionario().getCargo().getId()));
        
        return pagamentosPorCargo.entrySet().stream()
            .map(entry -> {
                Long cargoId = entry.getKey();
                List<FolhaPagamento> pagamentos = entry.getValue();
                
                String descricao = pagamentos.get(0).getFuncionario().getCargo().getDescricao();
                Long quantidadeFuncionarios = pagamentos.stream()
                    .map(fp -> fp.getFuncionario().getId())
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaPagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal valorMedio = quantidadeFuncionarios > 0 ? 
                    valorTotal.divide(BigDecimal.valueOf(quantidadeFuncionarios), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
                
                return new CargoStatsDTO(cargoId, descricao, quantidadeFuncionarios, valorMedio, valorTotal);
            })
            .collect(Collectors.toList());
    }
    
    private BigDecimal calcularTotalProventos(List<FolhaPagamento> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "PROVENTO".equals(fp.getRubrica().getTipoRubrica().getDescricao()))
            .map(FolhaPagamento::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calcularTotalDescontos(List<FolhaPagamento> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "DESCONTO".equals(fp.getRubrica().getTipoRubrica().getDescricao()))
            .map(FolhaPagamento::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private List<RubricaStatsDTO> calcularTopProventos(List<FolhaPagamento> folhaCompetencia) {
        Map<Long, List<FolhaPagamento>> proventosPorRubrica = folhaCompetencia.stream()
            .filter(fp -> "PROVENTO".equals(fp.getRubrica().getTipoRubrica().getDescricao()))
            .collect(Collectors.groupingBy(fp -> fp.getRubrica().getId()));
        
        return proventosPorRubrica.entrySet().stream()
            .map(entry -> {
                Long rubricaId = entry.getKey();
                List<FolhaPagamento> proventos = entry.getValue();
                BigDecimal valorTotal = proventos.stream()
                    .map(FolhaPagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new RubricaStatsDTO(
                    rubricaId,
                    proventos.get(0).getRubrica().getCodigo(),
                    proventos.get(0).getRubrica().getDescricao(),
                    valorTotal,
                    (long) proventos.size()
                );
            })
            .sorted((a, b) -> b.valorTotal().compareTo(a.valorTotal()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    private List<RubricaStatsDTO> calcularTopDescontos(List<FolhaPagamento> folhaCompetencia) {
        Map<Long, List<FolhaPagamento>> descontosPorRubrica = folhaCompetencia.stream()
            .filter(fp -> "DESCONTO".equals(fp.getRubrica().getTipoRubrica().getDescricao()))
            .collect(Collectors.groupingBy(fp -> fp.getRubrica().getId()));
        
        return descontosPorRubrica.entrySet().stream()
            .map(entry -> {
                Long rubricaId = entry.getKey();
                List<FolhaPagamento> descontos = entry.getValue();
                BigDecimal valorTotal = descontos.stream()
                    .map(FolhaPagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new RubricaStatsDTO(
                    rubricaId,
                    descontos.get(0).getRubrica().getCodigo(),
                    descontos.get(0).getRubrica().getDescricao(),
                    valorTotal,
                    (long) descontos.size()
                );
            })
            .sorted((a, b) -> b.valorTotal().compareTo(a.valorTotal()))
            .limit(5)
            .collect(Collectors.toList());
    }

    private List<EvolucaoMensalDTO> calcularEvolucaoMensal() {
        // Buscar dados dos últimos 12 meses
        LocalDate dataInicio = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        List<ResumoFolhaPagamento> resumos = resumoFolhaPagamentoRepository.findUltimos12Meses(dataInicio);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");
        
        return resumos.stream()
            .map(resumo -> new EvolucaoMensalDTO(
                resumo.getCompetenciaInicio().format(formatter),
                resumo.getTotalLiquido(),
                resumo.getTotalEmpregados()
            ))
            .collect(Collectors.toList());
    }
} 