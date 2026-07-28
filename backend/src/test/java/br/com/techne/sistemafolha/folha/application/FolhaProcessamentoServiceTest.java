package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaProcessamentoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private FichaMensalRepository fichaMensalRepository;

    @Mock
    private FichaLinhaRepository fichaLinhaRepository;

    @InjectMocks
    private FolhaProcessamentoService folhaProcessamentoService;

    @Test
    void processar_copiaAdpParaFichaComOperadoresSnapshot() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento();
        Rubrica desconto = rubricaDesconto();

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FolhaPagamento linhaInss = linhaAdp(funcionario, desconto, new BigDecimal("800.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario, linhaInss));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(1, resultado.totalFichas());
        assertEquals(2, resultado.totalLinhas());
        assertEquals(1, resultado.totalFuncionarios());

        verify(fichaMensalRepository).deleteByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, org.mockito.Mockito.times(2)).save(linhaCaptor.capture());
        FichaLinha linhaDesconto = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOperadorLiquido() == -1)
            .findFirst()
            .orElseThrow();
        assertEquals(OrigemLinha.FOLHA_ADP, linhaDesconto.getOrigemLinha());
        assertEquals((short) 0, linhaDesconto.getOperadorBruto());
        assertEquals((short) -1, linhaDesconto.getOperadorLiquido());

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10000.00"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("9200.00"), fichaFinal.getLiquido());
        assertEquals(new BigDecimal("10000.00"), fichaFinal.getCustoFolha());
    }

    private Funcionario funcionario(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("Funcionário " + id);
        return funcionario;
    }

    private Rubrica rubricaProvento() {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(1L);
        rubrica.setTipoRubrica(tipo);
        rubrica.setOperadorBruto((short) 1);
        rubrica.setOperadorLiquido((short) 1);
        rubrica.setOperadorCusto((short) 1);
        return rubrica;
    }

    private Rubrica rubricaDesconto() {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("DESCONTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(2L);
        rubrica.setTipoRubrica(tipo);
        rubrica.setOperadorBruto((short) 0);
        rubrica.setOperadorLiquido((short) -1);
        rubrica.setOperadorCusto((short) 0);
        return rubrica;
    }

    private FolhaPagamento linhaAdp(Funcionario funcionario, Rubrica rubrica, BigDecimal valor) {
        FolhaPagamento linha = new FolhaPagamento();
        linha.setFuncionario(funcionario);
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setDataInicio(COMPETENCIA_INICIO);
        linha.setDataFim(COMPETENCIA_FIM);
        linha.setAtivo(true);
        linha.setDecimoTerceiro(false);
        return linha;
    }
}
