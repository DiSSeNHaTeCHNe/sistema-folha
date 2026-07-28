package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioLinhaSnapshot;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.folha.api.FichaLinhaDetalheDTO;
import br.com.techne.sistemafolha.folha.api.Totalizador;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FichaMensalNotFoundException;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaFichaConsultaServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final Long FICHA_ID = 100L;
    private static final Long FUNCIONARIO_ID = 10L;
    private static final Long CENTRO_A = 20L;
    private static final Long CENTRO_B = 30L;
    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FichaMensalRepository fichaMensalRepository;

    @Mock
    private FichaLinhaRepository fichaLinhaRepository;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @InjectMocks
    private FolhaFichaConsultaService folhaFichaConsultaService;

    @Test
    void listarLinhasPorTotalizador_gross_filtraOperadorZero() {
        stubUsuario(contextoRestrito(Set.of(CENTRO_A)));
        FichaMensal ficha = ficha(CENTRO_A);
        when(fichaMensalRepository.findByIdAtivoWithFuncionario(FICHA_ID)).thenReturn(Optional.of(ficha));
        when(fichaLinhaRepository.findByFichaMensalIdAndAtivoTrue(FICHA_ID))
            .thenReturn(List.of(
                linha("001", "Salário", (short) 1, (short) 1, (short) 1, new BigDecimal("10000.00"), OrigemLinha.FOLHA_ADP),
                linha("002", "INSS", (short) 0, (short) -1, (short) 0, new BigDecimal("800.00"), OrigemLinha.FOLHA_ADP)
            ));

        List<FichaLinhaDetalheDTO> result = folhaFichaConsultaService.listarLinhasPorTotalizador(
            LOGIN, FICHA_ID, Totalizador.GROSS);

        assertEquals(1, result.size());
        assertEquals("001", result.get(0).rubricaCodigo());
        assertEquals(new BigDecimal("10000.00"), result.get(0).contribuicao());
        verify(beneficioConsultaPort, never()).findLinhasPorFuncionarioECompetencia(
            eq(FUNCIONARIO_ID), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
    }

    @Test
    void listarLinhasPorTotalizador_net_incluiProventoEDesconto() {
        stubUsuario(contextoRestrito(Set.of(CENTRO_A)));
        FichaMensal ficha = ficha(CENTRO_A);
        when(fichaMensalRepository.findByIdAtivoWithFuncionario(FICHA_ID)).thenReturn(Optional.of(ficha));
        when(fichaLinhaRepository.findByFichaMensalIdAndAtivoTrue(FICHA_ID))
            .thenReturn(List.of(
                linha("001", "Salário", (short) 1, (short) 1, (short) 1, new BigDecimal("10000.00"), OrigemLinha.FOLHA_ADP),
                linha("002", "INSS", (short) 0, (short) -1, (short) 0, new BigDecimal("800.00"), OrigemLinha.FOLHA_ADP)
            ));

        List<FichaLinhaDetalheDTO> result = folhaFichaConsultaService.listarLinhasPorTotalizador(
            LOGIN, FICHA_ID, Totalizador.NET);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(l -> l.rubricaCodigo().equals("001")));
        assertTrue(result.stream().anyMatch(l -> l.rubricaCodigo().equals("002")));
    }

    @Test
    void listarLinhasPorTotalizador_companyCost_incluiFichaLinhasEBeneficios() {
        stubUsuario(contextoRestrito(Set.of(CENTRO_A)));
        FichaMensal ficha = ficha(CENTRO_A);
        when(fichaMensalRepository.findByIdAtivoWithFuncionario(FICHA_ID)).thenReturn(Optional.of(ficha));
        when(fichaLinhaRepository.findByFichaMensalIdAndAtivoTrue(FICHA_ID))
            .thenReturn(List.of(
                linha("001", "Salário", (short) 1, (short) 1, (short) 1, new BigDecimal("10000.00"), OrigemLinha.FOLHA_ADP),
                linha("900", "Ajuda", (short) 1, (short) 1, (short) 1, new BigDecimal("500.00"), OrigemLinha.CUSTO_FIXO)
            ));
        when(beneficioConsultaPort.findLinhasPorFuncionarioECompetencia(
            FUNCIONARIO_ID, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(new BeneficioLinhaSnapshot(1L, "VR", "Vale Refeição", new BigDecimal("600.00"))));

        List<FichaLinhaDetalheDTO> result = folhaFichaConsultaService.listarLinhasPorTotalizador(
            LOGIN, FICHA_ID, Totalizador.COMPANY_COST);

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(l -> "FOLHA_ADP".equals(l.origemLinha())));
        assertTrue(result.stream().anyMatch(l -> "CUSTO_FIXO".equals(l.origemLinha())));
        assertTrue(result.stream().anyMatch(l -> "BENEFICIO".equals(l.origemLinha()) && "VR".equals(l.rubricaCodigo())));
    }

    @Test
    void listarLinhasPorTotalizador_scopedForaDoEscopo_retorna404() {
        stubUsuario(contextoRestrito(Set.of(CENTRO_A)));
        FichaMensal ficha = ficha(CENTRO_B);
        when(fichaMensalRepository.findByIdAtivoWithFuncionario(FICHA_ID)).thenReturn(Optional.of(ficha));

        assertThrows(FichaMensalNotFoundException.class, () ->
            folhaFichaConsultaService.listarLinhasPorTotalizador(LOGIN, FICHA_ID, Totalizador.GROSS));

        verify(fichaLinhaRepository, never()).findByFichaMensalIdAndAtivoTrue(FICHA_ID);
    }

    @Test
    void listarLinhasPorTotalizador_fichaInexistente_retorna404() {
        stubUsuario(contextoAcessoTotal());
        when(fichaMensalRepository.findByIdAtivoWithFuncionario(FICHA_ID)).thenReturn(Optional.empty());

        assertThrows(FichaMensalNotFoundException.class, () ->
            folhaFichaConsultaService.listarLinhasPorTotalizador(LOGIN, FICHA_ID, Totalizador.GROSS));
    }

    private void stubUsuario(AccessContextDTO contexto) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private FichaMensal ficha(Long centroCustoId) {
        CentroCusto centro = new CentroCusto();
        centro.setId(centroCustoId);
        Funcionario funcionario = new Funcionario();
        funcionario.setId(FUNCIONARIO_ID);
        funcionario.setCentroCusto(centro);
        FichaMensal ficha = new FichaMensal();
        ficha.setId(FICHA_ID);
        ficha.setFuncionario(funcionario);
        ficha.setCompetenciaInicio(COMPETENCIA_INICIO);
        ficha.setCompetenciaFim(COMPETENCIA_FIM);
        ficha.setAtivo(true);
        return ficha;
    }

    private FichaLinha linha(
            String codigo, String descricao,
            short ob, short ol, short oc,
            BigDecimal valor, OrigemLinha origem) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(codigo);
        rubrica.setDescricao(descricao);
        rubrica.setTipoRubrica(tipo);
        FichaLinha linha = new FichaLinha();
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setOperadorBruto(ob);
        linha.setOperadorLiquido(ol);
        linha.setOperadorCusto(oc);
        linha.setOrigemLinha(origem);
        linha.setAtivo(true);
        return linha;
    }
}
