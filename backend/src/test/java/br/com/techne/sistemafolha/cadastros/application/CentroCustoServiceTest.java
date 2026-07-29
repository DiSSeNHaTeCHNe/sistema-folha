package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.CentroCustoDTO;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CentroCustoServiceTest {

    @Mock
    private CentroCustoRepository centroCustoRepository;

    @Mock
    private LinhaNegocioRepository linhaNegocioRepository;

    @InjectMocks
    private CentroCustoService centroCustoService;

    @Test
    void listarTodas_retornaAtivos() {
        CentroCusto cc = centroCusto(1L, "Dev", true, 10L);
        when(centroCustoRepository.findByAtivoTrue()).thenReturn(List.of(cc));

        List<CentroCustoDTO> result = centroCustoService.listarTodas();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Dev", result.get(0).descricao());
        assertEquals(10L, result.get(0).linhaNegocioId());
    }

    @Test
    void listarPorLinhaNegocio_filtraPorLinha() {
        CentroCusto cc = centroCusto(2L, "QA", true, 10L);
        when(centroCustoRepository.findByLinhaNegocioIdAndAtivoTrue(10L)).thenReturn(List.of(cc));

        List<CentroCustoDTO> result = centroCustoService.listarPorLinhaNegocio(10L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
        verify(centroCustoRepository).findByLinhaNegocioIdAndAtivoTrue(10L);
    }

    @Test
    void buscarPorId_retornaDtoQuandoAtivo() {
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCusto(1L, "Dev", true, 10L)));

        CentroCustoDTO result = centroCustoService.buscarPorId(1L);

        assertEquals(1L, result.id());
        assertEquals("Dev", result.descricao());
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoInativo() {
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCusto(1L, "Dev", false, 10L)));

        assertThrows(CentroCustoNotFoundException.class, () -> centroCustoService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoNaoEncontrado() {
        when(centroCustoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CentroCustoNotFoundException.class, () -> centroCustoService.buscarPorId(99L));
    }

    @Test
    void cadastrar_persisteComLinhaNegocioAtiva() {
        LinhaNegocio ln = linhaNegocio(10L, "TI", true);
        CentroCustoDTO dto = new CentroCustoDTO(null, "Desenvolvimento", null, 10L);
        when(linhaNegocioRepository.findById(10L)).thenReturn(Optional.of(ln));
        when(centroCustoRepository.save(any(CentroCusto.class))).thenAnswer(inv -> {
            CentroCusto cc = inv.getArgument(0);
            cc.setId(5L);
            return cc;
        });

        CentroCustoDTO result = centroCustoService.cadastrar(dto);

        assertEquals(5L, result.id());
        assertEquals("Desenvolvimento", result.descricao());
        assertEquals(true, result.ativo());
        assertEquals(10L, result.linhaNegocioId());
        verify(centroCustoRepository).save(any(CentroCusto.class));
    }

    @Test
    void cadastrar_lancaExcecaoQuandoLinhaNegocioInexistente() {
        CentroCustoDTO dto = new CentroCustoDTO(null, "Dev", null, 99L);
        when(linhaNegocioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LinhaNegocioNotFoundException.class, () -> centroCustoService.cadastrar(dto));
        verify(centroCustoRepository, never()).save(any());
    }

    @Test
    void cadastrar_lancaExcecaoQuandoLinhaNegocioInativa() {
        when(linhaNegocioRepository.findById(10L)).thenReturn(Optional.of(linhaNegocio(10L, "TI", false)));

        assertThrows(LinhaNegocioNotFoundException.class,
            () -> centroCustoService.cadastrar(new CentroCustoDTO(null, "Dev", null, 10L)));
        verify(centroCustoRepository, never()).save(any());
    }

    @Test
    void atualizar_alteraDescricaoELinha() {
        CentroCusto cc = centroCusto(1L, "Dev", true, 10L);
        LinhaNegocio lnNova = linhaNegocio(20L, "Ops", true);
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(cc));
        when(linhaNegocioRepository.findById(20L)).thenReturn(Optional.of(lnNova));
        when(centroCustoRepository.save(cc)).thenReturn(cc);

        CentroCustoDTO dto = new CentroCustoDTO(1L, "Desenvolvimento", true, 20L);
        CentroCustoDTO result = centroCustoService.atualizar(1L, dto);

        assertEquals("Desenvolvimento", result.descricao());
        assertEquals(20L, result.linhaNegocioId());
        verify(centroCustoRepository).save(cc);
    }

    @Test
    void atualizar_lancaExcecaoQuandoCentroNaoEncontrado() {
        when(centroCustoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CentroCustoNotFoundException.class,
            () -> centroCustoService.atualizar(99L, new CentroCustoDTO(99L, "X", true, 10L)));
        verify(centroCustoRepository, never()).save(any());
    }

    @Test
    void remover_desativaCentro() {
        CentroCusto cc = centroCusto(1L, "Dev", true, 10L);
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(cc));
        when(centroCustoRepository.save(cc)).thenReturn(cc);

        centroCustoService.remover(1L);

        assertFalse(cc.getAtivo());
        verify(centroCustoRepository).save(cc);
    }

    @Test
    void remover_lancaExcecaoQuandoNaoEncontrado() {
        when(centroCustoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CentroCustoNotFoundException.class, () -> centroCustoService.remover(99L));
        verify(centroCustoRepository, never()).save(any());
    }

    private LinhaNegocio linhaNegocio(Long id, String descricao, boolean ativo) {
        LinhaNegocio ln = new LinhaNegocio();
        ln.setId(id);
        ln.setDescricao(descricao);
        ln.setAtivo(ativo);
        return ln;
    }

    private CentroCusto centroCusto(Long id, String descricao, boolean ativo, Long linhaNegocioId) {
        CentroCusto cc = new CentroCusto();
        cc.setId(id);
        cc.setDescricao(descricao);
        cc.setAtivo(ativo);
        cc.setLinhaNegocio(linhaNegocio(linhaNegocioId, "LN", true));
        return cc;
    }
}
