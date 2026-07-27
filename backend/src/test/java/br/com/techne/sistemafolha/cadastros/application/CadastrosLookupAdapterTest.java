package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private LinhaNegocio linhaNegocio(Long id) {
        LinhaNegocio linha = new LinhaNegocio();
        linha.setId(id);
        return linha;
    }
}
