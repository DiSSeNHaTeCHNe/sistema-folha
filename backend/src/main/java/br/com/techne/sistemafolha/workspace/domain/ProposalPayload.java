package br.com.techne.sistemafolha.workspace.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ProposalPayload {

    private String kind;
    private String nome;
    private List<DatasetFieldSchema> campos = new ArrayList<>();
    private String tipoWidget;
    private List<WidgetSourceRef> fontes = new ArrayList<>();
    private String formula;
    private Map<String, Object> config = new HashMap<>();
    private Long templateId;
    private Long workspaceId;
    private String descricao;
    private String dedupHash;
}
