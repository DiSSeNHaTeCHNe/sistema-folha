package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.LinhaNegocioDTO;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocioNotFoundException;
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
class LinhaNegocioServiceTest {

    @Mock
    private LinhaNegocioRepository linhaNegocioRepository;

    @InjectMocks
    private LinhaNegocioService linhaNegocioService;

    @Test
    void listarTodas_retornaAtivas() {
        LinhaNegocio ln = linhaNegocio(1L, "TI", true);
        when(linhaNegocioRepository.findByAtivoTrue()).thenReturn(List.of(ln));

        List<LinhaNegocioDTO> result = linhaNegocioService.listarTodas();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("TI", result.get(0).descricao());
    }

    @Test
    void buscarPorId_retornaDtoQuandoAtivo() {
        when(linhaNegocioRepository.findById(1L)).thenReturn(Optional.of(linhaNegocio(1L, "TI", true)));

        LinhaNegocioDTO result = linhaNegocioService.buscarPorId(1L);

        assertEquals(1L, result.id());
        assertEquals("TI", result.descricao());
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoInativo() {
        when(linhaNegocioRepository.findById(1L)).thenReturn(Optional.of(linhaNegocio(1L, "TI", false)));

        assertThrows(LinhaNegocioNotFoundException.class, () -> linhaNegocioService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoNaoEncontrado() {
        when(linhaNegocioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LinhaNegocioNotFoundException.class, () -> linhaNegocioService.buscarPorId(99L));
    }

    @Test
    void cadastrar_persisteLinhaAtiva() {
        LinhaNegocioDTO dto = new LinhaNegocioDTO(null, "Operações", null);
        when(linhaNegocioRepository.save(any(LinhaNegocio.class))).thenAnswer(inv -> {
            LinhaNegocio ln = inv.getArgument(0);
            ln.setId(5L);
            return ln;
        });

        LinhaNegocioDTO result = linhaNegocioService.cadastrar(dto);

        assertEquals(5L, result.id());
        assertEquals("Operações", result.descricao());
        assertEquals(true, result.ativo());
        verify(linhaNegocioRepository).save(any(LinhaNegocio.class));
    }

    @Test
    void atualizar_alteraDescricao() {
        LinhaNegocio ln = linhaNegocio(1L, "TI", true);
        when(linhaNegocioRepository.findById(1L)).thenReturn(Optional.of(ln));
        when(linhaNegocioRepository.save(ln)).thenReturn(ln);

        LinhaNegocioDTO result = linhaNegocioService.atualizar(1L, new LinhaNegocioDTO(1L, "Tecnologia", true));

        assertEquals("Tecnologia", result.descricao());
        assertEquals("Tecnologia", ln.getDescricao());
        verify(linhaNegocioRepository).save(ln);
    }

    @Test
    void atualizar_lancaExcecaoQuandoNaoEncontrado() {
        when(linhaNegocioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LinhaNegocioNotFoundException.class,
            () -> linhaNegocioService.atualizar(99L, new LinhaNegocioDTO(99L, "X", true)));
        verify(linhaNegocioRepository, never()).save(any());
    }

    @Test
    void remover_desativaLinha() {
        LinhaNegocio ln = linhaNegocio(1L, "TI", true);
        when(linhaNegocioRepository.findById(1L)).thenReturn(Optional.of(ln));
        when(linhaNegocioRepository.save(ln)).thenReturn(ln);

        linhaNegocioService.remover(1L);

        assertFalse(ln.getAtivo());
        verify(linhaNegocioRepository).save(ln);
    }

    @Test
    void remover_lancaExcecaoQuandoNaoEncontrado() {
        when(linhaNegocioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LinhaNegocioNotFoundException.class, () -> linhaNegocioService.remover(99L));
        verify(linhaNegocioRepository, never()).save(any());
    }

    private LinhaNegocio linhaNegocio(Long id, String descricao, boolean ativo) {
        LinhaNegocio ln = new LinhaNegocio();
        ln.setId(id);
        ln.setDescricao(descricao);
        ln.setAtivo(ativo);
        return ln;
    }
}
