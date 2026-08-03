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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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

    @Test
    void listarTodas_retornaAtivas() {
        when(rubricaRepository.findByAtivoTrue()).thenReturn(List.of(rubricaAtiva("001", "Salário")));

        List<RubricaDTO> result = rubricaService.listarTodas();

        assertEquals(1, result.size());
        assertEquals("001", result.get(0).getCodigo());
    }

    @Test
    void buscarPorId_retornaDto() {
        Rubrica rubrica = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(rubrica));

        RubricaDTO result = rubricaService.buscarPorId(1L);

        assertEquals("001", result.getCodigo());
    }

    @Test
    void buscarPorId_naoEncontrada_lancaExcecao() {
        when(rubricaRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        RubricaNotFoundException ex = assertThrows(RubricaNotFoundException.class,
                () -> rubricaService.buscarPorId(99L));
        assertEquals("Rubrica não encontrada com ID: 99", ex.getMessage());
    }

    @Test
    void cadastrar_codigoDuplicado_lancaIllegalArgumentException() {
        when(rubricaRepository.existsByCodigo("001")).thenReturn(true);

        RubricaDTO dto = new RubricaDTO(null, "001", "Salário", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void atualizar_alteraRubrica() {
        Rubrica existente = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> inv.getArgument(0));

        RubricaDTO dto = new RubricaDTO(1L, "001", "Salário Base", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) 1, true);
        RubricaDTO result = rubricaService.atualizar(1L, dto);

        assertEquals("Salário Base", result.getDescricao());
    }

    @Test
    void remover_softDelete() {
        Rubrica rubrica = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(rubrica));

        rubricaService.remover(1L);

        verify(rubricaRepository).softDelete(1L);
    }

    @Test
    void cadastrar_tipoDesconto_aplicaOperadoresPadrao() {
        when(rubricaRepository.existsByCodigo("9001")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("DESCONTO")).thenReturn(Optional.of(tipoRubrica("DESCONTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "9001", "INSS", "DESCONTO", "DESCONTO", null,
            null, null, null, true);
        RubricaDTO result = rubricaService.cadastrar(dto);

        assertEquals((short) 0, result.getOperadorBruto());
        assertEquals((short) -1, result.getOperadorLiquido());
        assertEquals((short) 0, result.getOperadorCusto());
    }

    @Test
    void listar_statusTodos_passaAtivoNull() {
        when(rubricaRepository.findByFiltros(null, null, null)).thenReturn(List.of(rubricaAtiva("001", "Salário")));

        rubricaService.listar(null, null, br.com.techne.sistemafolha.cadastros.api.RubricaStatusFiltro.TODOS);

        verify(rubricaRepository).findByFiltros(null, null, null);
    }

    @Test
    void atualizar_codigoAlteradoDuplicado_lancaExcecao() {
        Rubrica existente = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
        when(rubricaRepository.existsByCodigo("002")).thenReturn(true);

        RubricaDTO dto = new RubricaDTO(1L, "002", "Outro", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.atualizar(1L, dto));
    }

    @Test
    void cadastrar_operadoresParciais_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("003")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "003", "Teste", "PROVENTO", "PROVENTO", null,
            (short) 1, null, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_tipoOutro_aplicaOperadoresZero() {
        when(rubricaRepository.existsByCodigo("004")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("INFORMATIVO")).thenReturn(Optional.of(tipoRubrica("INFORMATIVO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(4L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "004", "Info", "INFORMATIVO", "INFORMATIVO", null,
            null, null, null, null);
        RubricaDTO result = rubricaService.cadastrar(dto);

        assertEquals((short) 0, result.getOperadorBruto());
        assertTrue(result.getAtivo());
    }

    @Test
    void cadastrar_semTipoRubrica_aplicaOperadoresZero() {
        when(rubricaRepository.existsByCodigo("005")).thenReturn(false);
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "005", "Sem tipo", null, null, null,
            null, null, null, true);
        RubricaDTO result = rubricaService.cadastrar(dto);

        assertEquals((short) 0, result.getOperadorCusto());
    }

    @Test
    void cadastrar_tipoNaoEncontrado_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("006")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("INVALIDO")).thenReturn(Optional.empty());

        RubricaDTO dto = new RubricaDTO(null, "006", "X", "INVALIDO", "INVALIDO", null,
            (short) 1, (short) 1, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void listarTodas_rubricaSemTipo_mapeiaNull() {
        Rubrica rubrica = rubricaAtiva("001", "Salário");
        rubrica.setTipoRubrica(null);
        when(rubricaRepository.findByAtivoTrue()).thenReturn(List.of(rubrica));

        RubricaDTO result = rubricaService.listarTodas().get(0);

        assertEquals(null, result.getTipoRubricaDescricao());
    }

    @Test
    void listar_statusNull_usaAtivoTrue() {
        when(rubricaRepository.findByFiltros(null, null, true)).thenReturn(List.of());

        rubricaService.listar(null, null, null);

        verify(rubricaRepository).findByFiltros(null, null, true);
    }

    @Test
    void atualizar_mesmoCodigo_naoVerificaDuplicidade() {
        Rubrica existente = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> inv.getArgument(0));

        RubricaDTO dto = new RubricaDTO(1L, "001", "Salário Base", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) 1, true);
        rubricaService.atualizar(1L, dto);

        verify(rubricaRepository, never()).existsByCodigo(any());
    }

    @Test
    void atualizar_codigoAlteradoSemConflito_persiste() {
        Rubrica existente = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
        when(rubricaRepository.existsByCodigo("002")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> inv.getArgument(0));

        RubricaDTO dto = new RubricaDTO(1L, "002", "Novo", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) 1, true);

        assertEquals("002", rubricaService.atualizar(1L, dto).getCodigo());
    }

    @Test
    void cadastrar_tipoComDescricaoNull_aplicaOperadoresZero() {
        when(rubricaRepository.existsByCodigo("009")).thenReturn(false);
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao(null);
        when(tipoRubricaRepository.findByDescricao("X")).thenReturn(Optional.of(tipo));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(9L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "009", "X", "X", "X", null, null, null, null, true);
        assertEquals((short) 0, rubricaService.cadastrar(dto).getOperadorLiquido());
    }

    @Test
    void cadastrar_todosOperadoresExplicitos_persiste() {
        when(rubricaRepository.existsByCodigo("010")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        RubricaDTO dto = new RubricaDTO(null, "010", "Custom", "PROVENTO", "PROVENTO", null,
            (short) -1, (short) -1, (short) -1, true);
        RubricaDTO result = rubricaService.cadastrar(dto);

        assertEquals((short) -1, result.getOperadorCusto());
    }

    @Test
    void cadastrar_operadorInvalido_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("007")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "007", "X", "PROVENTO", "PROVENTO", null,
            (short) 2, (short) 1, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_apenasUmOperadorInformado_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("008")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "008", "X", "PROVENTO", "PROVENTO", null,
            (short) 1, null, null, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_operadorLiquidoInvalido_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("011")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "011", "X", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 5, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_operadorCustoInvalido_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("012")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "012", "X", "PROVENTO", "PROVENTO", null,
            (short) 1, (short) 1, (short) -2, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void atualizar_comOperadoresExplicitos_persiste() {
        Rubrica existente = rubricaAtiva("001", "Salário");
        when(rubricaRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(existente));
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> inv.getArgument(0));

        RubricaDTO dto = new RubricaDTO(1L, "001", "Salário", "PROVENTO", "PROVENTO", null,
            (short) -1, (short) 0, (short) 1, true);

        RubricaDTO result = rubricaService.atualizar(1L, dto);

        assertEquals((short) -1, result.getOperadorBruto());
        assertEquals((short) 0, result.getOperadorLiquido());
    }

    @Test
    void cadastrar_apenasOperadorBrutoInformado_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("013")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "013", "X", "PROVENTO", "PROVENTO", null,
            (short) 1, null, null, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_apenasOperadorLiquidoInformado_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("014")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "014", "X", "PROVENTO", "PROVENTO", null,
            null, (short) 1, null, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
    }

    @Test
    void cadastrar_apenasOperadorCustoInformado_lancaExcecao() {
        when(rubricaRepository.existsByCodigo("015")).thenReturn(false);
        when(tipoRubricaRepository.findByDescricao("PROVENTO")).thenReturn(Optional.of(tipoRubrica("PROVENTO")));

        RubricaDTO dto = new RubricaDTO(null, "015", "X", "PROVENTO", "PROVENTO", null,
            null, null, (short) 1, true);

        assertThrows(IllegalArgumentException.class, () -> rubricaService.cadastrar(dto));
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
