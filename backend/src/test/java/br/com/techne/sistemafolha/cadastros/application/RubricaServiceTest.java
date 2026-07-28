package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.RubricaStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.TipoRubricaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

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

    private Rubrica rubricaAtiva(String codigo, String descricao) {
        TipoRubrica tipoRubrica = new TipoRubrica();
        tipoRubrica.setDescricao("Provento");

        Rubrica rubrica = new Rubrica();
        rubrica.setId(1L);
        rubrica.setCodigo(codigo);
        rubrica.setDescricao(descricao);
        rubrica.setTipoRubrica(tipoRubrica);
        rubrica.setAtivo(true);
        return rubrica;
    }
}
