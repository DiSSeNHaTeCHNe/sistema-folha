package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoLinhaCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoResumoCommand;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaImportacaoAdapterTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private FolhaImportacaoAdapter adapter;

    private Funcionario funcionario;
    private Rubrica rubrica;
    private Cargo cargo;
    private CentroCusto centroCusto;
    private LinhaNegocio linhaNegocio;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João");

        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");

        rubrica = new Rubrica();
        rubrica.setId(2L);
        rubrica.setCodigo("0010");
        rubrica.setDescricao("Salário");
        rubrica.setTipoRubrica(tipo);

        cargo = new Cargo();
        cargo.setId(3L);
        cargo.setDescricao("Analista");

        centroCusto = new CentroCusto();
        centroCusto.setId(4L);
        centroCusto.setDescricao("CC Alpha");

        linhaNegocio = new LinhaNegocio();
        linhaNegocio.setId(5L);
        linhaNegocio.setDescricao("LN Teste");

        when(entityManager.getReference(Funcionario.class, 1L)).thenReturn(funcionario);
        when(entityManager.getReference(Rubrica.class, 2L)).thenReturn(rubrica);
        lenient().when(entityManager.getReference(Cargo.class, 3L)).thenReturn(cargo);
        lenient().when(entityManager.getReference(CentroCusto.class, 4L)).thenReturn(centroCusto);
        lenient().when(entityManager.getReference(LinhaNegocio.class, 5L)).thenReturn(linhaNegocio);
    }

    @Test
    void persistirImportacao_semSubstituir_persisteLinhasESalvaResumo() {
        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false, List.of(linhaCommand()), resumoCommand());

        when(folhaPagamentoRepository.save(any(FolhaPagamento.class))).thenAnswer(inv -> {
            FolhaPagamento f = inv.getArgument(0);
            f.setId(99L);
            return f;
        });

        List<FolhaPagamentoDTO> result = adapter.persistirImportacao(command);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).id());
        verify(folhaPagamentoRepository, never()).deleteAll(anyList());
        verify(resumoFolhaPagamentoRepository).save(any(ResumoFolhaPagamento.class));
    }

    @Test
    void persistirImportacao_comSubstituir_removeAntesDeInserir() {
        ResumoFolhaPagamento resumoAntigo = new ResumoFolhaPagamento();
        resumoAntigo.setId(50L);
        FolhaPagamento folhaAntiga = new FolhaPagamento();
        folhaAntiga.setId(60L);

        when(resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(resumoAntigo));
        when(folhaPagamentoRepository.findByDataInicioAndDataFimAndDecimoTerceiro(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(folhaAntiga));
        when(folhaPagamentoRepository.save(any(FolhaPagamento.class))).thenAnswer(inv -> {
            FolhaPagamento f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, true,
            List.of(linhaCommand()), resumoCommand());

        List<FolhaPagamentoDTO> result = adapter.persistirImportacao(command);

        assertNotNull(result);
        verify(folhaPagamentoRepository).deleteAll(List.of(folhaAntiga));
        verify(resumoFolhaPagamentoRepository).delete(resumoAntigo);
    }

    @Test
    void persistirImportacao_resumoNull_naoSalvaResumo() {
        when(folhaPagamentoRepository.save(any(FolhaPagamento.class))).thenAnswer(inv -> {
            FolhaPagamento f = inv.getArgument(0);
            f.setId(101L);
            return f;
        });

        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false,
            List.of(linhaCommand()), null);

        adapter.persistirImportacao(command);

        verify(resumoFolhaPagamentoRepository, never()).save(any(ResumoFolhaPagamento.class));
    }

    @Test
    void persistirImportacao_cargoECentroCustoNull_persisteSemGetReferenceOpcional() {
        ArgumentCaptor<FolhaPagamento> captor = ArgumentCaptor.forClass(FolhaPagamento.class);
        when(folhaPagamentoRepository.save(captor.capture())).thenAnswer(inv -> {
            FolhaPagamento f = inv.getArgument(0);
            f.setId(102L);
            return f;
        });

        FolhaImportacaoLinhaCommand linhaSemOpcionais = new FolhaImportacaoLinhaCommand(
            1L, 2L, null, null, null,
            new BigDecimal("5000.00"),
            new BigDecimal("1.00"),
            new BigDecimal("5000.00"));

        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false,
            List.of(linhaSemOpcionais), null);

        List<FolhaPagamentoDTO> result = adapter.persistirImportacao(command);

        assertEquals(1, result.size());
        FolhaPagamento salva = captor.getValue();
        assertNotNull(salva.getFuncionario());
        assertNotNull(salva.getRubrica());
        assertNull(salva.getCargo());
        assertNull(salva.getCentroCusto());
        assertNull(salva.getLinhaNegocio());
        verify(entityManager, never()).getReference(eq(Cargo.class), isNull());
        verify(entityManager, never()).getReference(eq(CentroCusto.class), isNull());
        verify(entityManager, never()).getReference(eq(LinhaNegocio.class), isNull());
    }

    @Test
    void persistirImportacao_decimoTerceiro_gravaFlagNaLinha() {
        ArgumentCaptor<FolhaPagamento> captor = ArgumentCaptor.forClass(FolhaPagamento.class);
        when(folhaPagamentoRepository.save(captor.capture())).thenAnswer(inv -> {
            FolhaPagamento f = inv.getArgument(0);
            f.setId(103L);
            return f;
        });

        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, true, false,
            List.of(linhaCommand()), null);

        adapter.persistirImportacao(command);

        assertTrue(captor.getValue().getDecimoTerceiro());
    }

    private FolhaImportacaoLinhaCommand linhaCommand() {
        return new FolhaImportacaoLinhaCommand(
            1L, 2L, 3L, 4L, 5L,
            new BigDecimal("5000.00"),
            new BigDecimal("1.00"),
            new BigDecimal("5000.00"));
    }

    private FolhaImportacaoResumoCommand resumoCommand() {
        return new FolhaImportacaoResumoCommand(
            10,
            new BigDecimal("1000.00"),
            new BigDecimal("60000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("55000.00"));
    }
}
