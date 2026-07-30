package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRubricaFixaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastrosLookupAdapterTest {

    @Mock
    private CentroCustoRepository centroCustoRepository;

    @Mock
    private LinhaNegocioRepository linhaNegocioRepository;

    @Mock
    private RubricaRepository rubricaRepository;

    @Mock
    private FuncionarioRubricaFixaRepository funcionarioRubricaFixaRepository;

    @InjectMocks
    private CadastrosLookupAdapter adapter;

    @Test
    void findCentroCustoById_centroPresente_retornaOptionalComCentro() {
        CentroCusto centro = centroCusto(10L);
        when(centroCustoRepository.findById(10L)).thenReturn(Optional.of(centro));

        Optional<CentroCusto> result = adapter.findCentroCustoById(10L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
    }

    @Test
    void findCentroCustoById_centroAusente_retornaOptionalVazio() {
        when(centroCustoRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<CentroCusto> result = adapter.findCentroCustoById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findLinhaNegocioById_linhaPresente_retornaOptionalComLinha() {
        LinhaNegocio linha = linhaNegocio(20L);
        when(linhaNegocioRepository.findById(20L)).thenReturn(Optional.of(linha));

        Optional<LinhaNegocio> result = adapter.findLinhaNegocioById(20L);

        assertTrue(result.isPresent());
        assertEquals(20L, result.get().getId());
    }

    @Test
    void findLinhaNegocioById_linhaAusente_retornaOptionalVazio() {
        when(linhaNegocioRepository.findById(88L)).thenReturn(Optional.empty());

        Optional<LinhaNegocio> result = adapter.findLinhaNegocioById(88L);

        assertTrue(result.isEmpty());
    }

    private CentroCusto centroCusto(Long id) {
        CentroCusto centro = new CentroCusto();
        centro.setId(id);
        return centro;
    }

    @Test
    void findRubricaAtivaByCodigo_rubricaAtiva_retornaOptional() {
        Rubrica rubrica = rubrica("001", true);
        when(rubricaRepository.findByCodigo("001")).thenReturn(Optional.of(rubrica));

        Optional<Rubrica> result = adapter.findRubricaAtivaByCodigo("001");

        assertTrue(result.isPresent());
        assertEquals("001", result.get().getCodigo());
    }

    @Test
    void findRubricaAtivaByCodigo_rubricaInativa_retornaVazio() {
        when(rubricaRepository.findByCodigo("002")).thenReturn(Optional.of(rubrica("002", false)));

        assertTrue(adapter.findRubricaAtivaByCodigo("002").isEmpty());
    }

    @Test
    void findRubricasFixasVigentesNaCompetencia_delegaParaRepository() {
        LocalDate inicio = LocalDate.of(2024, 10, 1);
        LocalDate fim = LocalDate.of(2024, 10, 31);
        FuncionarioRubricaFixa fixa = new FuncionarioRubricaFixa();
        when(funcionarioRubricaFixaRepository.findVigentesNaCompetencia(inicio, fim))
            .thenReturn(List.of(fixa));

        List<FuncionarioRubricaFixa> result = adapter.findRubricasFixasVigentesNaCompetencia(inicio, fim);

        assertEquals(1, result.size());
    }

    private LinhaNegocio linhaNegocio(Long id) {
        LinhaNegocio linha = new LinhaNegocio();
        linha.setId(id);
        return linha;
    }

    private Rubrica rubrica(String codigo, boolean ativo) {
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(codigo);
        rubrica.setAtivo(ativo);
        return rubrica;
    }
}
