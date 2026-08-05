package br.com.techne.sistemafolha.workspace.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateStructurePayload {

    private Long sourceId;
    private String kind;
    private String nome;
    private List<DatasetFieldSchema> schema = new ArrayList<>();
    private String tipo;
    private List<WidgetSourceRef> fontes = new ArrayList<>();
    private String formula;
    private Map<String, Object> config = new HashMap<>();
}
