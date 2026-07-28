package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaProcessamentoService {

    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final FichaMensalRepository fichaMensalRepository;
    private final FichaLinhaRepository fichaLinhaRepository;

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

        int totalLinhas = 0;
        List<FichaMensal> fichas = new ArrayList<>();

        for (List<FolhaPagamento> grupo : porFuncionario.values()) {
            FichaMensal ficha = montarFicha(grupo, competenciaInicio, competenciaFim, decimoTerceiro);
            ficha = fichaMensalRepository.save(ficha);

            List<FolhaMotorCalculo.LinhaCalculoInput> inputsMotor = new ArrayList<>();
            for (FolhaPagamento linhaAdp : grupo) {
                FichaLinha fichaLinha = montarLinhaAdp(ficha, linhaAdp);
                fichaLinhaRepository.save(fichaLinha);
                totalLinhas++;
                inputsMotor.add(toInput(fichaLinha));
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
        linha.setAtivo(true);
        return linha;
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FichaLinha linha) {
        return new FolhaMotorCalculo.LinhaCalculoInput(
            linha.getValor(),
            linha.getOperadorBruto(),
            linha.getOperadorLiquido(),
            linha.getOperadorCusto()
        );
    }
}
