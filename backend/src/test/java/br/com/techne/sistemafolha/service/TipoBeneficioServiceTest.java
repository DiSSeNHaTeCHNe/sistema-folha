package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.TipoBeneficioDTO;
import br.com.techne.sistemafolha.exception.TipoBeneficioCodigoDuplicadoException;
import br.com.techne.sistemafolha.exception.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.model.TipoBeneficio;
import br.com.techne.sistemafolha.repository.TipoBeneficioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoBeneficioServiceTest {

    @Mock
    private TipoBeneficioRepository tipoBeneficioRepository;

    @InjectMocks
    private TipoBeneficioService tipoBeneficioService;

    @Test
    void criar_persiste_tipo_ativo() {
        TipoBeneficioDTO dto = dtoBase(null, "VALE_REFEICAO", "Vale Refeição - Custo Empresa");
        when(tipoBeneficioRepository.existsByCodigo("VALE_REFEICAO")).thenReturn(false);
        when(tipoBeneficioRepository.save(any(TipoBeneficio.class))).thenAnswer(inv -> {
            TipoBeneficio tipo = inv.getArgument(0);
            tipo.setId(1L);
            return tipo;
        });

        TipoBeneficioDTO result = tipoBeneficioService.criar(dto);

        assertEquals(1L, result.id());
        assertEquals("VALE_REFEICAO", result.codigo());
        assertEquals("Vale Refeição - Custo Empresa", result.descricao());
        assertEquals(true, result.ativo());
        verify(tipoBeneficioRepository).save(any(TipoBeneficio.class));
    }

    @Test
    void criar_rejeita_codigo_duplicado() {
        TipoBeneficioDTO dto = dtoBase(null, "SEGUROS", "Seguros - Custo Empresa");
        when(tipoBeneficioRepository.existsByCodigo("SEGUROS")).thenReturn(true);

        TipoBeneficioCodigoDuplicadoException ex = assertThrows(
                TipoBeneficioCodigoDuplicadoException.class,
                () -> tipoBeneficioService.criar(dto));

        assertEquals("Já existe um tipo de benefício com o código: SEGUROS", ex.getMessage());
        verify(tipoBeneficioRepository, never()).save(any());
    }

    @Test
    void remover_desativa_tipo() {
        TipoBeneficio tipo = tipoAtivo(1L, "SEGUROS", "Seguros - Custo Empresa");
        when(tipoBeneficioRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(tipoBeneficioRepository.save(tipo)).thenReturn(tipo);

        tipoBeneficioService.remover(1L);

        assertFalse(tipo.getAtivo());
        verify(tipoBeneficioRepository).save(tipo);
    }

    @Test
    void remover_lanca_excecao_quando_nao_encontrado() {
        when(tipoBeneficioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TipoBeneficioNotFoundException.class, () -> tipoBeneficioService.remover(99L));
        verify(tipoBeneficioRepository, never()).save(any());
    }

    private TipoBeneficioDTO dtoBase(Long id, String codigo, String descricao) {
        return new TipoBeneficioDTO(id, codigo, descricao, true);
    }

    private TipoBeneficio tipoAtivo(Long id, String codigo, String descricao) {
        TipoBeneficio tipo = new TipoBeneficio();
        tipo.setId(id);
        tipo.setCodigo(codigo);
        tipo.setDescricao(descricao);
        tipo.setAtivo(true);
        return tipo;
    }
}
