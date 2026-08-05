package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SystemFieldCatalog {

    private static final Map<String, List<String>> SYSTEM_FIELDS = Map.of(
        "FOLHA", List.of("total_proventos", "total_descontos", "total_liquido"),
        "BENEFICIO", List.of("total_beneficios"),
        "ORCAMENTO", List.of("orcado", "realizado", "variacao")
    );

    public List<AvailableField> fieldsForSource(String ref) {
        if (ref == null || ref.isBlank()) {
            return List.of();
        }
        List<String> names = SYSTEM_FIELDS.get(ref.toUpperCase());
        if (names == null) {
            return List.of();
        }
        return names.stream().map(AvailableField::new).toList();
    }
}
