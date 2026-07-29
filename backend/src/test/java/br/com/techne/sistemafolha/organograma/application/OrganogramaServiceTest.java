package br.com.techne.sistemafolha.organograma.application;

import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.organograma.api.NoOrganogramaDTO;
import br.com.techne.sistemafolha.organograma.domain.NoOrganograma;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
