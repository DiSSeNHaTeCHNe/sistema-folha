package br.com.techne.sistemafolha.organograma.application;

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
