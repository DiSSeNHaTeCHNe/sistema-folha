package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaTotalizacaoAdapterTest {

    @Mock
    private FolhaTotalizacaoService folhaTotalizacaoService;

    @InjectMocks
    private FolhaTotalizacaoAdapter adapter;

    @Test
    void calcularTotalCustoEmpresa_delegaParaService() {
        LocalDate inicio = LocalDate.of(2024, 10, 1);
        LocalDate fim = LocalDate.of(2024, 10, 31);
        List<FolhaLinhaSnapshot> linhas = List.of();
        AccessContextDTO contexto = new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
        BigDecimal esperado = new BigDecimal("12345.67");

        when(folhaTotalizacaoService.calcularTotalCustoEmpresa(linhas, inicio, fim, contexto))
            .thenReturn(esperado);

        BigDecimal result = adapter.calcularTotalCustoEmpresa(linhas, inicio, fim, contexto);

        assertEquals(esperado, result);
        verify(folhaTotalizacaoService).calcularTotalCustoEmpresa(linhas, inicio, fim, contexto);
    }
}
