package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.RubricaDTO;
import br.com.techne.sistemafolha.cadastros.api.RubricaStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.TipoRubricaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RubricaServiceTest {

    @Mock
    private RubricaRepository rubricaRepository;

    @Mock
    private TipoRubricaRepository tipoRubricaRepository;

    @InjectMocks
    private RubricaService rubricaService;

    @Test
    void listar_default_ativo_passes_ativo_true_to_repository() {
        when(rubricaRepository.findByFiltros(isNull(), isNull(), eq(true)))
                .thenReturn(Collections.emptyList());

        rubricaService.listar(null, null, RubricaStatusFiltro.ATIVO);

        verify(rubricaRepository).findByFiltros(isNull(), isNull(), eq(true));
    }

    @Test
    void listar_inativo_passes_ativo_false_to_repository() {
        when(rubricaRepository.findByFiltros(isNull(), isNull(), eq(false)))
                .thenReturn(Collections.emptyList());

        rubricaService.listar(null, null, RubricaStatusFiltro.INATIVO);

        verify(rubricaRepository).findByFiltros(isNull(), isNull(), eq(false));
    }

    @Test
    void listar_todos_passes_ativo_null_to_repository() {
        when(rubricaRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        rubricaService.listar(null, null, RubricaStatusFiltro.TODOS);

        verify(rubricaRepository).findByFiltros(isNull(), isNull(), isNull());
    }

    @Test
    void listar_codigo_passes_ilike_pattern_to_repository() {
        when(rubricaRepository.findByFiltros(eq("%ABC%"), isNull(), eq(true)))
                .thenReturn(Collections.emptyList());

        rubricaService.listar("ABC", null, RubricaStatusFiltro.ATIVO);

        verify(rubricaRepository).findByFiltros(eq("%ABC%"), isNull(), eq(true));
    }

    @Test
    void listar_descricao_passes_ilike_pattern_to_repository() {
        when(rubricaRepository.findByFiltros(isNull(), eq("%foo%"), eq(true)))
                .thenReturn(Collections.emptyList());

        rubricaService.listar(null, "foo", RubricaStatusFiltro.ATIVO);

        verify(rubricaRepository).findByFiltros(isNull(), eq("%foo%"), eq(true));
    }

    @Test
    void listar_trim_ignora_espacos_em_branco() {
        when(rubricaRepository.findByFiltros(isNull(), isNull(), eq(true)))
                .thenReturn(Collections.emptyList());

        rubricaService.listar("   ", "  ", RubricaStatusFiltro.ATIVO);

        verify(rubricaRepository).findByFiltros(isNull(), isNull(), eq(true));
    }

    @Test
    void listar_combined_filters_delegates_to_repository() {
        Rubrica rubrica = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByFiltros(eq("%001%"), eq("%Sal%"), eq(true)))
                .thenReturn(List.of(rubrica));

        rubricaService.listar("001", "Sal", RubricaStatusFiltro.ATIVO);

        verify(rubricaRepository).findByFiltros(eq("%001%"), eq("%Sal%"), eq(true));
    }

    @Test
    void cadastrar_comOperadoresValidos_persisteOperadores() {
        TipoRubrica tipoDesconto = tipoRubrica("DESCONTO");
        when(rubricaRepository.existsByCodigo("9001")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("DESCONTO")).thenReturn(Optional.of(tipoDesconto));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "9001", "INSS", "DESCONTO", "DESCONTO", null,
            (short) 0, (short) -1, (short) 0, true);

        RubricaDTO resultado = rubricaService.cadastrar(dto);

        assertEquals((short) 0, resultado.getOperadorBruto());
        assertEquals((short) -1, resultado.getOperadorLiquido());
        assertEquals((short) 0, resultado.getOperadorCusto());

        ArgumentCaptor<Rubrica> captor = ArgumentCaptor.forClass(Rubrica.class);
        verify(rubricaRepository).save(captor.capture());
        assertEquals((short) -1, captor.getValue().getOperadorLiquido());
    }

    @Test
    void cadastrar_operadorInvalido_lancaIllegalArgumentException() {
        TipoRubrica tipoProvento = tipoRubrica("PROVENTO");
        when(rubricaRepository.existsByCodigo("1000")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoProvento));

        RubricaDTO dto = new RubricaDTO(null, "1000", "Salário", "PROVENTO", "PROVENTO", null,
            (short) 2, (short) 1, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_semOperadores_derivaDeTipoRubrica() {
        TipoRubrica tipoProvento = tipoRubrica("PROVENTO");
        when(rubricaRepository.existsByCodigo("1000")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoProvento));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "1000", "Salário", "PROVENTO", "PROVENTO", null,
            null, null, null, true);

        RubricaDTO resultado = rubricaService.cadastrar(dto);

        assertEquals((short) 1, resultado.getOperadorBruto());
        assertEquals((short) 1, resultado.getOperadorLiquido());
        assertEquals((short) 1, resultado.getOperadorCusto());
    }

    private TipoRubrica tipoRubrica(String descricao) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao(descricao);
        return tipo;
    }

    private Rubrica rubricaAtiva(String codigo, String descricao) {
        TipoRubrica tipoRubrica = tipoRubrica("Provento");

        Rubrica rubrica = new Rubrica();
        rubrica.setId(1L);
        rubrica.setCodigo(codigo);
        rubrica.setDescricao(descricao);
        rubrica.setTipoRubrica(tipoRubrica);
        rubrica.setOperadorBruto((short) 1);
        rubrica.setOperadorLiquido((short) 1);
        rubrica.setOperadorCusto((short) 1);
        rubrica.setAtivo(true);
        return rubrica;
    }
}
