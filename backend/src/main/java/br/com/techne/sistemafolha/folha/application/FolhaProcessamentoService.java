package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaProcessamentoService {

    private static final Logger logger = LoggerFactory.getLogger(FolhaProcessamentoService.class);
    private static final String DOMAIN = "folha";
    private static final String CODIGO_RUBRICA_FERIAS = "5000";
    private static final BigDecimal FATOR_FERIAS = new BigDecimal("2.5");
    private static final BigDecimal MESES_ANO = new BigDecimal("12");

    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final FichaMensalRepository fichaMensalRepository;
    private final FichaLinhaRepository fichaLinhaRepository;
    private final CadastrosLookupPort cadastrosLookupPort;

    @Transactional
    public ProcessamentoResultadoDTO processar(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro,
            ProcessamentoOpcoes opcoes) {
        fichaMensalRepository.deleteByCompetencia(competenciaInicio, competenciaFim, decimoTerceiro);

        List<FolhaPagamento> linhasAdp = folhaPagamentoRepository
            .findByCompetenciaAndDecimoTerceiroAndAtivoTrue(competenciaInicio, competenciaFim, decimoTerceiro);

        Map<Long, List<FolhaPagamento>> porFuncionario = linhasAdp.stream()
            .collect(Collectors.groupingBy(l -> l.getFuncionario().getId()));

        Map<Long, List<FuncionarioRubricaFixa>> fixosPorFuncionario = cadastrosLookupPort
            .findRubricasFixasVigentesNaCompetencia(competenciaInicio, competenciaFim)
            .stream()
            .collect(Collectors.groupingBy(f -> f.getFuncionario().getId()));

        int totalLinhas = 0;
        List<FichaMensal> fichas = new ArrayList<>();
        ProcessamentoOpcoes opcoesEfetivas = opcoes != null ? opcoes : new ProcessamentoOpcoes(false);

        for (List<FolhaPagamento> grupo : porFuncionario.values()) {
            Funcionario funcionario = grupo.get(0).getFuncionario();
            if (!isFuncionarioClt(funcionario)) {
                continue;
            }

            FichaMensal ficha = montarFicha(grupo, competenciaInicio, competenciaFim, decimoTerceiro);
            ficha = fichaMensalRepository.save(ficha);

            List<FolhaMotorCalculo.LinhaCalculoInput> inputsMotor = new ArrayList<>();
            Set<Long> rubricasAdp = new HashSet<>();

            for (FolhaPagamento linhaAdp : grupo) {
                FichaLinha fichaLinha = montarLinhaAdp(ficha, linhaAdp);
                fichaLinhaRepository.save(fichaLinha);
                totalLinhas++;
                rubricasAdp.add(linhaAdp.getRubrica().getId());
                inputsMotor.add(toInput(fichaLinha));
            }

            List<FuncionarioRubricaFixa> fixos = fixosPorFuncionario.getOrDefault(funcionario.getId(), List.of());
            for (FuncionarioRubricaFixa fixo : fixos) {
                if (rubricasAdp.contains(fixo.getRubrica().getId())) {
                    logger.warn("{}Rubrica fixa ignorada (duplicata ADP): funcionario={}, rubrica={}",
                        DomainLogging.prefix(DOMAIN), funcionario.getId(), fixo.getRubrica().getCodigo());
                    continue;
                }
                FichaLinha linhaFixa = montarLinhaCustoFixo(ficha, fixo);
                fichaLinhaRepository.save(linhaFixa);
                totalLinhas++;
                inputsMotor.add(toInput(linhaFixa));
            }

            if (opcoesEfetivas.recalcularFerias()) {
                FichaLinha linhaFerias = montarLinhaFeriasCalculada(ficha, inputsMotor);
                if (linhaFerias != null) {
                    fichaLinhaRepository.save(linhaFerias);
                    totalLinhas++;
                    inputsMotor.add(toInput(linhaFerias));
                }
            }

            FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(inputsMotor);
            ficha.setBruto(totais.bruto());
            ficha.setLiquido(totais.liquido());
            ficha.setCustoFolha(totais.custoFolha());
            fichaMensalRepository.save(ficha);
            fichas.add(ficha);
        }

        return new ProcessamentoResultadoDTO(fichas.size(), totalLinhas, porFuncionario.size());
    }

    private boolean isFuncionarioClt(Funcionario funcionario) {
        if (funcionario.getRegimeTrabalho() == null) {
            return true;
        }
        return "CLT".equals(funcionario.getRegimeTrabalho().getCodigo())
            && Boolean.TRUE.equals(funcionario.getRegimeTrabalho().getAtivo());
    }

    private FichaMensal montarFicha(
            List<FolhaPagamento> grupo,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro) {
        Funcionario funcionario = grupo.get(0).getFuncionario();
        FichaMensal ficha = new FichaMensal();
        ficha.setFuncionario(funcionario);
        ficha.setCompetenciaInicio(competenciaInicio);
        ficha.setCompetenciaFim(competenciaFim);
        ficha.setDecimoTerceiro(decimoTerceiro);
        ficha.setAtivo(true);
        return ficha;
    }

    private FichaLinha montarLinhaAdp(FichaMensal ficha, FolhaPagamento linhaAdp) {
        Rubrica rubrica = linhaAdp.getRubrica();
        FichaLinha linha = new FichaLinha();
        linha.setFichaMensal(ficha);
        linha.setRubrica(rubrica);
        linha.setValor(linhaAdp.getValor());
        linha.setOrigemLinha(OrigemLinha.FOLHA_ADP);
        linha.setOperadorBruto(rubrica.getOperadorBruto());
        linha.setOperadorLiquido(rubrica.getOperadorLiquido());
        linha.setOperadorCusto(rubrica.getOperadorCusto());
        linha.setPorcentagem(snapshotPorcentagem(rubrica.getPorcentagem()));
        linha.setAtivo(true);
        return linha;
    }

    private FichaLinha montarLinhaCustoFixo(FichaMensal ficha, FuncionarioRubricaFixa fixo) {
        Rubrica rubrica = fixo.getRubrica();
        FichaLinha linha = new FichaLinha();
        linha.setFichaMensal(ficha);
        linha.setRubrica(rubrica);
        linha.setValor(fixo.getValor());
        linha.setOrigemLinha(OrigemLinha.CUSTO_FIXO);
        linha.setOperadorBruto(rubrica.getOperadorBruto());
        linha.setOperadorLiquido(rubrica.getOperadorLiquido());
        linha.setOperadorCusto(rubrica.getOperadorCusto());
        linha.setPorcentagem(snapshotPorcentagem(rubrica.getPorcentagem()));
        linha.setAtivo(true);
        return linha;
    }

    private FichaLinha montarLinhaFeriasCalculada(
            FichaMensal ficha, List<FolhaMotorCalculo.LinhaCalculoInput> inputsAtuais) {
        return cadastrosLookupPort.findRubricaAtivaByCodigo(CODIGO_RUBRICA_FERIAS)
            .map(rubrica -> {
                FolhaMotorCalculo.TotaisFuncionario parcial = FolhaMotorCalculo.calcularPorLinhas(inputsAtuais);
                BigDecimal valorFerias = parcial.bruto()
                    .multiply(FATOR_FERIAS)
                    .divide(MESES_ANO, 2, RoundingMode.HALF_UP);

                FichaLinha linha = new FichaLinha();
                linha.setFichaMensal(ficha);
                linha.setRubrica(rubrica);
                linha.setValor(valorFerias);
                linha.setOrigemLinha(OrigemLinha.CALCULADO);
                linha.setOperadorBruto(rubrica.getOperadorBruto());
                linha.setOperadorLiquido(rubrica.getOperadorLiquido());
                linha.setOperadorCusto(rubrica.getOperadorCusto());
                linha.setPorcentagem(snapshotPorcentagem(rubrica.getPorcentagem()));
                linha.setAtivo(true);
                return linha;
            })
            .orElse(null);
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FichaLinha linha) {
        return new FolhaMotorCalculo.LinhaCalculoInput(
            linha.getValor(),
            linha.getOperadorBruto(),
            linha.getOperadorLiquido(),
            linha.getOperadorCusto(),
            linha.getPorcentagem()
        );
    }

    private BigDecimal snapshotPorcentagem(Double porcentagem) {
        return porcentagem != null ? BigDecimal.valueOf(porcentagem) : null;
    }
}
