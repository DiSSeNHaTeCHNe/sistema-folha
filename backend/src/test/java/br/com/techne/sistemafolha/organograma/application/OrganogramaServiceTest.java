package br.com.techne.sistemafolha.organograma.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.organograma.api.NoOrganogramaCreateDTO;
import br.com.techne.sistemafolha.organograma.api.NoOrganogramaDTO;
import br.com.techne.sistemafolha.organograma.domain.NoOrganograma;
import br.com.techne.sistemafolha.organograma.domain.NoOrganogramaNotFoundException;
import br.com.techne.sistemafolha.organograma.infrastructure.CentroCustoOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.FuncionarioOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.NoOrganogramaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganogramaServiceTest {

    @Mock
    private NoOrganogramaRepository noOrganogramaRepository;

    @Mock
    private FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;

    @Mock
    private CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private CadastrosLookupPort cadastrosLookupPort;

    @InjectMocks
    private OrganogramaService organogramaService;

    @Test
    @SuppressWarnings("unchecked")
    void construirArvore_ignoraNoComDtoNull() {
        List<NoOrganograma> nos = new ArrayList<>();
        nos.add(null);
        nos.add(noAtivo(1L, null, 0));

        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(nos.get(1)))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(nos.get(1)))
            .thenReturn(Collections.emptyList());

        List<NoOrganogramaDTO> result = (List<NoOrganogramaDTO>) ReflectionTestUtils.invokeMethod(
            organogramaService, "construirArvore", nos);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void obterArvoreCompleta_retornaArvoreSemNpe() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        NoOrganograma filho = noAtivo(2L, raiz, 1);

        when(noOrganogramaRepository.findByOrganogramaAtivoTrueAndAtivoTrue())
            .thenReturn(List.of(raiz, filho));
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(raiz))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(raiz))
            .thenReturn(Collections.emptyList());
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(filho))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(filho))
            .thenReturn(Collections.emptyList());

        List<NoOrganogramaDTO> result = organogramaService.obterArvoreCompleta();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(1, result.get(0).children().size());
        assertEquals(2L, result.get(0).children().get(0).id());
    }

    @Test
    void obterArvoreCompleta_semNosAtivos_retornaVazio() {
        when(noOrganogramaRepository.findByOrganogramaAtivoTrueAndAtivoTrue())
            .thenReturn(Collections.emptyList());

        List<NoOrganogramaDTO> result = organogramaService.obterArvoreCompleta();

        assertTrue(result.isEmpty());
    }

    @Test
    void cadastrar_noRaiz_calculaPosicaoAutomaticamente() {
        NoOrganogramaCreateDTO dto = new NoOrganogramaCreateDTO("Diretoria", "Raiz", null, null);
        NoOrganograma salvo = noAtivo(1L, null, 0);
        salvo.setNome("Diretoria");
        salvo.setDescricao("Raiz");
        salvo.setPosicao(0);

        when(noOrganogramaRepository.findByParentIsNullAndAtivoTrueOrderByPosicao())
            .thenReturn(Collections.emptyList());
        when(noOrganogramaRepository.save(any(NoOrganograma.class))).thenReturn(salvo);
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(salvo))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(salvo))
            .thenReturn(Collections.emptyList());

        NoOrganogramaDTO result = organogramaService.cadastrar(dto);

        assertEquals("Diretoria", result.nome());
        assertEquals(0, result.nivel());
        assertNull(result.parentId());
        verify(noOrganogramaRepository).save(any(NoOrganograma.class));
    }

    @Test
    void cadastrar_comParent_defineNivelDoPaiMaisUm() {
        NoOrganograma parent = noAtivo(1L, null, 0);
        NoOrganogramaCreateDTO dto = new NoOrganogramaCreateDTO("Gerência", null, 1L, null);
        NoOrganograma salvo = noAtivo(2L, parent, 1);
        salvo.setNome("Gerência");

        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(parent));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(parent))
            .thenReturn(Collections.emptyList());
        when(noOrganogramaRepository.save(any(NoOrganograma.class))).thenReturn(salvo);
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(salvo))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(salvo))
            .thenReturn(Collections.emptyList());

        NoOrganogramaDTO result = organogramaService.cadastrar(dto);

        assertEquals(1, result.nivel());
        assertEquals(1L, result.parentId());
    }

    @Test
    void validarCicloHierarquico_atualizarComDescendenteComoPai_lancaIllegalArgument() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        NoOrganograma filho = noAtivo(2L, raiz, 1);
        NoOrganograma neto = noAtivo(3L, filho, 2);

        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(raiz));
        when(noOrganogramaRepository.findById(3L)).thenReturn(java.util.Optional.of(neto));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> organogramaService.atualizar(1L, new NoOrganogramaDTO(
                1L, "Raiz", null, 0, 3L, null, 0, true, true,
                null, null, null, null, new ArrayList<>(), null, null, null, null
            ))
        );

        assertTrue(ex.getMessage().contains("ciclo"));
    }

    @Test
    void obterOrganogramaAtivo_quandoExiste_retornaDto() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        raiz.setOrganogramaAtivo(true);

        when(noOrganogramaRepository.findByOrganogramaAtivoTrue()).thenReturn(java.util.Optional.of(raiz));
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(raiz))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(raiz))
            .thenReturn(Collections.emptyList());

        NoOrganogramaDTO result = organogramaService.obterOrganogramaAtivo();

        assertEquals(1L, result.id());
        assertTrue(result.organogramaAtivo());
    }

    @Test
    void obterOrganogramaAtivo_quandoNaoExiste_retornaNull() {
        when(noOrganogramaRepository.findByOrganogramaAtivoTrue()).thenReturn(java.util.Optional.empty());

        assertNull(organogramaService.obterOrganogramaAtivo());
    }

    @Test
    void cadastrar_parentInexistente_lancaNoOrganogramaNotFound() {
        NoOrganogramaCreateDTO dto = new NoOrganogramaCreateDTO("Filho", null, 99L, null);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(NoOrganogramaNotFoundException.class, () -> organogramaService.cadastrar(dto));
    }

    @Test
    void listarTodos_retornaListaConvertida() {
        NoOrganograma no = noAtivo(1L, null, 0);
        when(noOrganogramaRepository.findByAtivoTrue()).thenReturn(List.of(no));
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(no))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no))
            .thenReturn(Collections.emptyList());

        List<NoOrganogramaDTO> result = organogramaService.listarTodos();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void buscarPorId_existente_retornaDto() {
        NoOrganograma no = noAtivo(7L, null, 0);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(7L)).thenReturn(java.util.Optional.of(no));
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(no))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no))
            .thenReturn(Collections.emptyList());

        NoOrganogramaDTO result = organogramaService.buscarPorId(7L);

        assertEquals(7L, result.id());
    }

    @Test
    void buscarPorId_inexistente_lancaNoOrganogramaNotFound() {
        when(noOrganogramaRepository.findByIdAndAtivoTrue(404L)).thenReturn(java.util.Optional.empty());

        assertThrows(NoOrganogramaNotFoundException.class, () -> organogramaService.buscarPorId(404L));
    }

    @Test
    void remover_semFilhos_executaSoftDelete() {
        NoOrganograma no = noAtivo(1L, null, 0);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(no));
        when(noOrganogramaRepository.existsByParentAndAtivoTrue(no)).thenReturn(false);

        organogramaService.remover(1L);

        verify(funcionarioOrganogramaRepository).deleteByNoOrganograma(no);
        verify(centroCustoOrganogramaRepository).deleteByNoOrganograma(no);
        verify(noOrganogramaRepository).softDelete(1L);
    }

    @Test
    void remover_comFilhosAtivos_lancaIllegalState() {
        NoOrganograma no = noAtivo(1L, null, 0);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(no));
        when(noOrganogramaRepository.existsByParentAndAtivoTrue(no)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> organogramaService.remover(1L));
    }

    @Test
    void obterFilhos_parentNull_retornaRaizes() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        when(noOrganogramaRepository.findByParentIsNullAndAtivoTrueOrderByPosicao())
            .thenReturn(List.of(raiz));

        List<NoOrganogramaDTO> result = organogramaService.obterFilhos(null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void obterFilhos_comParent_retornaFilhos() {
        NoOrganograma filho = noAtivo(2L, noAtivo(1L, null, 0), 1);
        when(noOrganogramaRepository.findByParentIdAndAtivoTrueOrderByPosicao(1L))
            .thenReturn(List.of(filho));

        List<NoOrganogramaDTO> result = organogramaService.obterFilhos(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
    }

    @Test
    void moverNo_paraNovaRaiz_atualizaNivel() {
        NoOrganograma no = noAtivo(2L, noAtivo(1L, null, 0), 1);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(2L)).thenReturn(java.util.Optional.of(no));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no))
            .thenReturn(Collections.emptyList());
        when(noOrganogramaRepository.save(no)).thenReturn(no);
        when(funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(no))
            .thenReturn(Collections.emptyList());
        when(centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no))
            .thenReturn(Collections.emptyList());

        NoOrganogramaDTO result = organogramaService.moverNo(2L, null, 0);

        assertEquals(0, result.nivel());
    }

    @Test
    void ativarOrganograma_noRaiz_ativaArvore() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        NoOrganograma filho = noAtivo(2L, raiz, 1);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(raiz));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(raiz))
            .thenReturn(List.of(filho));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(filho))
            .thenReturn(Collections.emptyList());
        when(noOrganogramaRepository.save(any(NoOrganograma.class))).thenAnswer(inv -> inv.getArgument(0));

        organogramaService.ativarOrganograma(1L);

        verify(noOrganogramaRepository).desativarTodosOrganogramas();
        verify(noOrganogramaRepository).save(raiz);
        verify(noOrganogramaRepository).save(filho);
    }

    @Test
    void ativarOrganograma_noComPai_lancaIllegalArgument() {
        NoOrganograma filho = noAtivo(2L, noAtivo(1L, null, 0), 1);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(2L)).thenReturn(java.util.Optional.of(filho));

        assertThrows(IllegalArgumentException.class, () -> organogramaService.ativarOrganograma(2L));
    }

    @Test
    void desativarOrganograma_chamaRepositorio() {
        organogramaService.desativarOrganograma();
        verify(noOrganogramaRepository).desativarTodosOrganogramas();
    }

    @Test
    void associarFuncionario_sucesso_retornaDto() {
        NoOrganograma no = noAtivo(1L, null, 0);
        Funcionario funcionario = new Funcionario();
        funcionario.setId(10L);
        funcionario.setNome("João");
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(no));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(10L)).thenReturn(java.util.Optional.of(funcionario));
        when(funcionarioOrganogramaRepository.existsByFuncionarioAndNoOrganograma(funcionario, no))
            .thenReturn(false);
        when(funcionarioOrganogramaRepository.findByFuncionario(funcionario))
            .thenReturn(Collections.emptyList());
        when(funcionarioOrganogramaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(10L, organogramaService.associarFuncionario(1L, 10L).funcionarioId());
    }

    @Test
    void associarFuncionario_inexistente_lancaFuncionarioNotFound() {
        NoOrganograma no = noAtivo(1L, null, 0);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(no));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(10L)).thenReturn(java.util.Optional.empty());

        assertThrows(FuncionarioNotFoundException.class,
            () -> organogramaService.associarFuncionario(1L, 10L));
    }

    @Test
    void associarCentroCusto_sucesso_retornaDto() {
        NoOrganograma no = noAtivo(1L, null, 0);
        CentroCusto centro = new CentroCusto();
        centro.setId(20L);
        centro.setAtivo(true);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(no));
        when(cadastrosLookupPort.findCentroCustoById(20L)).thenReturn(java.util.Optional.of(centro));
        when(centroCustoOrganogramaRepository.existsByCentroCustoAndNoOrganograma(centro, no))
            .thenReturn(false);
        when(centroCustoOrganogramaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(20L, organogramaService.associarCentroCusto(1L, 20L).centroCustoId());
    }

    @Test
    void removerComFilhos_removeRecursivamente() {
        NoOrganograma raiz = noAtivo(1L, null, 0);
        NoOrganograma filho = noAtivo(2L, raiz, 1);
        when(noOrganogramaRepository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(raiz));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(raiz))
            .thenReturn(List.of(filho));
        when(noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(filho))
            .thenReturn(Collections.emptyList());

        organogramaService.removerComFilhos(1L);

        verify(noOrganogramaRepository).softDelete(2L);
        verify(noOrganogramaRepository).softDelete(1L);
    }

    private NoOrganograma noAtivo(Long id, NoOrganograma parent, int nivel) {
        NoOrganograma no = new NoOrganograma();
        no.setId(id);
        no.setNome("Nó " + id);
        no.setAtivo(true);
        no.setOrganogramaAtivo(true);
        no.setParent(parent);
        no.setNivel(nivel);
        no.setPosicao(0);
        return no;
    }
}
