package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceLayoutValidatorTest {

    private final WorkspaceLayoutValidator validator = new WorkspaceLayoutValidator();

    @Test
    void validar_maisDe30Widgets_lanca400() {
        List<WorkspaceWidgetPayload> widgets = java.util.stream.IntStream.range(0, 31)
            .mapToObj(i -> widget("inst" + i, i, 3, 1, null, 1L))
            .toList();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(widgets));
        assertTrue(ex.getMessage().contains("30"));
    }

    @Test
    void validar_colSpanForaDoLimite_lanca400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(List.of(widget("a", 0, 13, 1, null, 1L))));
        assertTrue(ex.getMessage().contains("colSpan"));
    }

    @Test
    void validar_rowSpanForaDoLimite_lanca400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(List.of(widget("a", 0, 3, 4, null, 1L))));
        assertTrue(ex.getMessage().contains("rowSpan"));
    }

    @Test
    void validar_ordemNegativa_lanca400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(List.of(widget("a", -1, 3, 1, null, 1L))));
        assertTrue(ex.getMessage().contains("ordem"));
    }

    @Test
    void validar_instanceIdDuplicado_lanca400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(List.of(
                widget("dup", 0, 3, 1, null, 1L),
                widget("dup", 1, 3, 1, null, 2L))));
        assertTrue(ex.getMessage().contains("duplicado"));
    }

    @Test
    void validar_semReferenciaWidget_lanca400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validar(List.of(
                new WorkspaceWidgetPayload("a", 0, 3, 1, null, null, null))));
        assertTrue(ex.getMessage().contains("widgetId") || ex.getMessage().contains("userWidgetDefinitionId"));
    }

    @Test
    void normalizarOrdem_reindexaSequencialmente() {
        List<WorkspaceWidgetPayload> input = List.of(
            widget("b", 5, 3, 1, null, 2L),
            widget("a", 2, 6, 2, "kpi-total-funcionarios", null));

        List<WorkspaceWidgetPayload> normalizado = validator.normalizarOrdem(input);

        assertEquals("a", normalizado.get(0).instanceId());
        assertEquals(0, normalizado.get(0).ordem());
        assertEquals("b", normalizado.get(1).instanceId());
        assertEquals(1, normalizado.get(1).ordem());
    }

    private WorkspaceWidgetPayload widget(
            String instanceId, int ordem, int colSpan, int rowSpan,
            String widgetId, Long userWidgetDefinitionId) {
        return new WorkspaceWidgetPayload(
            instanceId, ordem, colSpan, rowSpan, widgetId, userWidgetDefinitionId, Map.of());
    }
}
