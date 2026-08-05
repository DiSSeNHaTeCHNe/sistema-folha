package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Component
public class WorkspaceLayoutValidator {

    public void validar(List<WorkspaceWidgetPayload> widgets) {
        if (widgets == null) {
            throw new IllegalArgumentException("widgets: obrigatório");
        }
        if (widgets.size() > WorkspaceLimits.MAX_WIDGETS_PER_WORKSPACE) {
            throw new IllegalArgumentException("Limite de 30 widgets atingido");
        }
        Set<String> instanceIds = new HashSet<>();
        for (WorkspaceWidgetPayload widget : widgets) {
            validarColSpan(widget.colSpan());
            validarRowSpan(widget.rowSpan());
            validarOrdem(widget.ordem());
            if (widget.instanceId() == null || widget.instanceId().isBlank()) {
                throw new IllegalArgumentException("instanceId: obrigatório");
            }
            if (!instanceIds.add(widget.instanceId())) {
                throw new IllegalArgumentException("instanceId duplicado: " + widget.instanceId());
            }
            boolean hasCatalog = widget.widgetId() != null && !widget.widgetId().isBlank();
            boolean hasUser = widget.userWidgetDefinitionId() != null;
            if (!hasCatalog && !hasUser) {
                throw new IllegalArgumentException(
                    "widget deve referenciar widgetId de catálogo ou userWidgetDefinitionId");
            }
        }
    }

    public List<WorkspaceWidgetPayload> normalizarOrdem(List<WorkspaceWidgetPayload> widgets) {
        List<WorkspaceWidgetPayload> sorted = widgets.stream()
            .sorted(Comparator.comparingInt(w -> w.ordem() != null ? w.ordem() : 0))
            .toList();
        return IntStream.range(0, sorted.size())
            .mapToObj(i -> new WorkspaceWidgetPayload(
                sorted.get(i).instanceId(),
                i,
                sorted.get(i).colSpan(),
                sorted.get(i).rowSpan(),
                sorted.get(i).widgetId(),
                sorted.get(i).userWidgetDefinitionId(),
                sorted.get(i).config()))
            .toList();
    }

    private void validarColSpan(Integer colSpan) {
        if (colSpan != null && (colSpan < 1 || colSpan > 12)) {
            throw new IllegalArgumentException("colSpan deve estar entre 1 e 12");
        }
    }

    private void validarRowSpan(Integer rowSpan) {
        if (rowSpan != null && (rowSpan < 1 || rowSpan > 3)) {
            throw new IllegalArgumentException("rowSpan deve estar entre 1 e 3");
        }
    }

    private void validarOrdem(Integer ordem) {
        if (ordem != null && ordem < 0) {
            throw new IllegalArgumentException("ordem não pode ser negativa");
        }
    }
}
