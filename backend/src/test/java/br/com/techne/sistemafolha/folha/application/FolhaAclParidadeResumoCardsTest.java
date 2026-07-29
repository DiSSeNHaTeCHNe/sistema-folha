package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** FCLT-ACL-11: soma dos cards scoped deve igualar totais scoped do resumo na mesma competência. */
@ExtendWith(MockitoExtension.class)
class FolhaAclParidadeResumoCardsTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);
    private static final LocalDate ANO_2024_INICIO = LocalDate.of(2024, 1, 1);
    private static final LocalDate ANO_2024_FIM = LocalDate.of(2024, 12, 31);
    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final Long CENTRO_A = 10L;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @Mock
    private EncargosRateioService encargosRateioService;

    @Mock
    private FichaMensalRepository fichaMensalRepository;

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private CadastrosLookupPort cadastrosLookupPort;

    private ResumoFolhaPagamentoService resumoFolhaPagamentoService;
    private FolhaPagamentoService folhaPagamentoService;

    @BeforeEach
    void setUp() {
        FolhaTotalizacaoService folhaTotalizacaoService =
            new FolhaTotalizacaoService(beneficioConsultaPort);
        resumoFolhaPagamentoService = new ResumoFolhaPagamentoService(
            resumoFolhaPagamentoRepository,
            folhaConsultaPort,
            organogramaAcessoPort,
            usuarioLookupPort,
            beneficioConsultaPort,
            encargosRateioService,
            fichaMensalRepository);
        folhaPagamentoService = new FolhaPagamentoService(
            folhaPagamentoRepository,
            cadastrosLookupPort,
            usuarioLookupPort,
            organogramaAcessoPort,
            folhaTotalizacaoService,
            folhaConsultaPort,
            resumoFolhaPagamentoRepository);
    }

    @Test
    void scopedCards_somaBrutoLiquidoCustoEmpresa_igualResumoScoped_mesmaCompetencia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));

        List<FolhaLinhaSnapshot> linhasScoped = List.of(
            linha(101L, CENTRO_A, "PROVENTO", "5000.00"),
            linha(101L, CENTRO_A, "DESCONTO", "500.00"),
            linha(102L, CENTRO_A, "PROVENTO", "3000.00"),
            linha(102L, CENTRO_A, "DESCONTO", "200.00")
        );

        ResumoFolhaPagamento snapshot = resumoAtivo(1L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A)))
            .thenReturn(linhasScoped);

        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                eq(101L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM)))
            .thenReturn(0);
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                eq(102L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM)))
            .thenReturn(0);

        List<ResumoFolhaPagamentoDTO> resumos =
            resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);
        List<FolhaTotaisFuncionarioDTO> cards = folhaPagamentoService.consultarTotaisPorFuncionario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(1, resumos.size());
        ResumoFolhaPagamentoDTO resumo = resumos.get(0);
        assertEquals(2, cards.size());

        BigDecimal sumBruto = cards.stream()
            .map(FolhaTotaisFuncionarioDTO::salBruto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumLiquido = cards.stream()
            .map(FolhaTotaisFuncionarioDTO::salLiquido)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumCustoEmpresa = cards.stream()
            .map(FolhaTotaisFuncionarioDTO::custoEmpresa)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, resumo.totalBruto().compareTo(sumBruto));
        assertEquals(0, resumo.totalLiquido().compareTo(sumLiquido));
        assertEquals(0, resumo.totalCustoEmpresa().compareTo(sumCustoEmpresa));
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private ResumoFolhaPagamento resumoAtivo(Long id) {
        ResumoFolhaPagamento resumo = new ResumoFolhaPagamento();
        resumo.setId(id);
        resumo.setTotalEmpregados(100);
        resumo.setTotalEncargos(new BigDecimal("10000.00"));
        resumo.setTotalPagamentos(new BigDecimal("60000.00"));
        resumo.setTotalDescontos(new BigDecimal("10000.00"));
        resumo.setTotalLiquido(new BigDecimal("50000.00"));
        resumo.setCompetenciaInicio(COMPETENCIA_INICIO);
        resumo.setCompetenciaFim(COMPETENCIA_FIM);
        resumo.setDataImportacao(LocalDateTime.of(2024, 11, 1, 8, 0));
        resumo.setDecimoTerceiro(false);
        resumo.setAtivo(true);
        return resumo;
    }

    private static FolhaLinhaSnapshot linha(Long funcionarioId, Long centroId, String tipo, String valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : ("PROVENTO".equals(tipo) ? (short) 1 : (short) 0);
        short oc = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func " + funcionarioId, centroId, "CC", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", tipo, new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP, null);
    }
}
