package br.com.techne.sistemafolha.organograma.acesso.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.organograma.domain.CentroCustoOrganograma;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.organograma.domain.FuncionarioOrganograma;
import br.com.techne.sistemafolha.organograma.domain.NoOrganograma;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.infrastructure.CentroCustoOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.FuncionarioOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.NoOrganogramaRepository;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganogramaAcessoServiceTest {

    private static final Long USUARIO_ID = 10L;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;

    @Mock
    private NoOrganogramaRepository noOrganogramaRepository;

    @Mock
    private CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;

    @InjectMocks
    private OrganogramaAcessoService service;

    @Test
    void obterContextoAcesso_semFuncionario_negaComMotivoSemFuncionario() {
        Usuario usuario = usuario(USUARIO_ID, null);
        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertFalse(contexto.temFuncionarioVinculado());
        assertFalse(contexto.temNoOrganograma());
        assertFalse(contexto.acessoTotal());
        assertTrue(contexto.centrosCustoIds().isEmpty());
        assertEquals(MotivoNegacaoAcesso.SEM_FUNCIONARIO, contexto.motivoNegacao());
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 99L));
    }

    @Test
    void obterContextoAcesso_comAcessoTotal_semFuncionario_concedeAcessoTotal() {
        Usuario usuario = usuario(USUARIO_ID, null, List.of(OrganogramaAcessoService.PERMISSAO_ACESSO_TOTAL));
        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertTrue(contexto.acessoTotal());
        assertFalse(contexto.temFuncionarioVinculado());
        assertFalse(contexto.temNoOrganograma());
        assertTrue(contexto.centrosCustoIds().isEmpty());
        assertNull(contexto.motivoNegacao());
        assertNull(contexto.noOrganogramaId());
        assertNull(contexto.noOrganogramaNome());
        assertNull(contexto.nivel());
        assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 999L));
    }

    @Test
    void obterContextoAcesso_somenteAdmin_semFuncionario_naoConcedeAcessoTotal() {
        Usuario usuario = usuario(USUARIO_ID, null, List.of("ADMIN"));
        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertFalse(contexto.acessoTotal());
        assertFalse(contexto.temFuncionarioVinculado());
        assertEquals(MotivoNegacaoAcesso.SEM_FUNCIONARIO, contexto.motivoNegacao());
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 99L));
    }

    @Test
    void obterContextoAcesso_funcionarioSemNo_negaComMotivoSemNoOrganograma() {
        Funcionario funcionario = funcionario(1L);
        Usuario usuario = usuario(USUARIO_ID, funcionario);
        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioOrganogramaRepository.findByFuncionarioWithNoAtivo(funcionario))
            .thenReturn(Collections.emptyList());

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertTrue(contexto.temFuncionarioVinculado());
        assertFalse(contexto.temNoOrganograma());
        assertFalse(contexto.acessoTotal());
        assertTrue(contexto.centrosCustoIds().isEmpty());
        assertEquals(MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA, contexto.motivoNegacao());
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 99L));
    }

    @Test
    void obterContextoAcesso_comNo_retornaCentrosDoNoEDescendentes() {
        Funcionario funcionario = funcionario(1L);
        Usuario usuario = usuario(USUARIO_ID, funcionario);
        NoOrganograma no = no(5L, "Diretoria", 1);
        FuncionarioOrganograma vinculo = vinculo(funcionario, no);
        CentroCusto centro = centro(100L);
        CentroCustoOrganograma centroNo = centroNoOrganograma(centro);

        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioOrganogramaRepository.findByFuncionarioWithNoAtivo(funcionario))
            .thenReturn(List.of(vinculo));
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no))
            .thenReturn(List.of(centroNo));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no))
            .thenReturn(Collections.emptyList());

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertTrue(contexto.temFuncionarioVinculado());
        assertTrue(contexto.temNoOrganograma());
        assertFalse(contexto.acessoTotal());
        assertEquals(Set.of(100L), contexto.centrosCustoIds());
        assertNull(contexto.motivoNegacao());
        assertEquals(5L, contexto.noOrganogramaId());
        assertEquals("Diretoria", contexto.noOrganogramaNome());
        assertEquals(1, contexto.nivel());
    }

    @Test
    void obterContextoAcesso_noComFilho_agregaCentrosDosDescendentes() {
        Funcionario funcionario = funcionario(1L);
        Usuario usuario = usuario(USUARIO_ID, funcionario);
        NoOrganograma pai = no(5L, "Diretoria", 1);
        NoOrganograma filho = no(6L, "Gerência", 2);
        filho.setParent(pai);
        FuncionarioOrganograma vinculo = vinculo(funcionario, pai);
        CentroCusto centroPai = centro(100L);
        CentroCusto centroFilho = centro(200L);

        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioOrganogramaRepository.findByFuncionarioWithNoAtivo(funcionario))
            .thenReturn(List.of(vinculo));
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(pai))
            .thenReturn(List.of(centroNoOrganograma(centroPai)));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(pai))
            .thenReturn(List.of(filho));
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(filho))
            .thenReturn(List.of(centroNoOrganograma(centroFilho)));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(filho))
            .thenReturn(Collections.emptyList());

        AccessContextDTO contexto = service.obterContextoAcesso(USUARIO_ID);

        assertEquals(Set.of(100L, 200L), contexto.centrosCustoIds());
        assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 100L));
        assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 200L));
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 300L));
    }

    @Test
    void usuarioPodeAcessarCentroCusto_escopoParcial_naoConcedeCentroForaDaSubarvore() {
        Funcionario funcionario = funcionario(1L);
        Usuario usuario = usuario(USUARIO_ID, funcionario);
        NoOrganograma noUsuario = no(5L, "Gerência A", 2);
        FuncionarioOrganograma vinculo = vinculo(funcionario, noUsuario);
        CentroCusto centroEscopo = centro(100L);

        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioOrganogramaRepository.findByFuncionarioWithNoAtivo(funcionario))
            .thenReturn(List.of(vinculo));
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(noUsuario))
            .thenReturn(List.of(centroNoOrganograma(centroEscopo)));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(noUsuario))
            .thenReturn(Collections.emptyList());

        assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 100L));
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 101L));
        assertEquals(Set.of(100L), service.obterCentrosCustoAcessiveis(USUARIO_ID));
    }

    @Test
    void usuarioPodeAcessarCentroCusto_comNo_verificaPertencimentoAoConjunto() {
        Funcionario funcionario = funcionario(1L);
        Usuario usuario = usuario(USUARIO_ID, funcionario);
        NoOrganograma no = no(5L, "Diretoria", 1);
        FuncionarioOrganograma vinculo = vinculo(funcionario, no);
        CentroCusto centro = centro(100L);
        CentroCustoOrganograma centroNo = centroNoOrganograma(centro);

        when(usuarioLookupPort.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioOrganogramaRepository.findByFuncionarioWithNoAtivo(funcionario))
            .thenReturn(List.of(vinculo));
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no))
            .thenReturn(List.of(centroNo));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no))
            .thenReturn(Collections.emptyList());

        assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 100L));
        assertFalse(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 200L));
        assertEquals(Set.of(100L), service.obterCentrosCustoAcessiveis(USUARIO_ID));
    }

    private Usuario usuario(Long id, Funcionario funcionario) {
        return usuario(id, funcionario, Collections.emptyList());
    }

    private Usuario usuario(Long id, Funcionario funcionario, List<String> permissoes) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setFuncionario(funcionario);
        usuario.setPermissoes(permissoes);
        return usuario;
    }

    private Funcionario funcionario(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        return funcionario;
    }

    private NoOrganograma no(Long id, String nome, int nivel) {
        NoOrganograma no = new NoOrganograma();
        no.setId(id);
        no.setNome(nome);
        no.setNivel(nivel);
        return no;
    }

    private FuncionarioOrganograma vinculo(Funcionario funcionario, NoOrganograma no) {
        FuncionarioOrganograma vinculo = new FuncionarioOrganograma();
        vinculo.setFuncionario(funcionario);
        vinculo.setNoOrganograma(no);
        return vinculo;
    }

    private CentroCusto centro(Long id) {
        CentroCusto centro = new CentroCusto();
        centro.setId(id);
        return centro;
    }

    private CentroCustoOrganograma centroNoOrganograma(CentroCusto centro) {
        CentroCustoOrganograma rel = new CentroCustoOrganograma();
        rel.setCentroCusto(centro);
        return rel;
    }
}
