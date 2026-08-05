package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.domain.WidgetInstancePayload;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

@Component
public class LayoutValidator {

    public static final int MAX_WIDGETS = 30;

    public void validarQuantidade(int count) {
        if (count > MAX_WIDGETS) {
            throw new IllegalArgumentException("Limite de 30 widgets atingido");
        }
    }

    public void validarColSpan(Integer colSpan) {
        if (colSpan != null && colSpan > 12) {
            throw new IllegalArgumentException("colSpan deve estar entre 1 e 12");
        }
    }

    public void validarRowSpan(Integer rowSpan) {
        if (rowSpan != null && (rowSpan < 1 || rowSpan > 3)) {
            throw new IllegalArgumentException("rowSpan deve estar entre 1 e 3");
        }
    }

    public void validarOrdem(Integer ordem) {
        if (ordem != null && ordem < 0) {
            throw new IllegalArgumentException("ordem não pode ser negativa");
        }
    }

    public void validarInstanceIdUnico(Set<String> instanceIds, String instanceId) {
        if (!instanceIds.add(instanceId)) {
            throw new IllegalArgumentException("instanceId duplicado: " + instanceId);
        }
    }

    public List<WidgetInstancePayload> normalizarOrdem(List<WidgetInstancePayload> widgets) {
        List<WidgetInstancePayload> sorted = widgets.stream()
            .sorted(Comparator.comparingInt(w -> w.ordem() != null ? w.ordem() : 0))
            .toList();
        return IntStream.range(0, sorted.size())
            .mapToObj(i -> new WidgetInstancePayload(
                sorted.get(i).widgetId(),
                sorted.get(i).instanceId(),
                i,
                sorted.get(i).colSpan(),
                sorted.get(i).rowSpan(),
                sorted.get(i).config()))
            .toList();
    }

    public <T> List<T> normalizarOrdemGenerico(
            List<T> widgets,
            java.util.function.Function<T, Integer> ordemFn,
            IntFunction<T> remapFn) {
        List<T> sorted = widgets.stream()
            .sorted(Comparator.comparingInt(w -> {
                Integer ordem = ordemFn.apply(w);
                return ordem != null ? ordem : 0;
            }))
            .toList();
        return IntStream.range(0, sorted.size())
            .mapToObj(remapFn)
            .toList();
    }

    public Set<String> novoConjuntoInstanceIds() {
        return new HashSet<>();
    }
}
