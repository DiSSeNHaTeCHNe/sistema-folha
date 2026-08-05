package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.OrcamentoInstallResultDTO;
import br.com.techne.sistemafolha.workspace.api.SaveWorkspaceLayoutRequest;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.ReferenciaEntidade;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrcamentoTemplateInstaller {

    static final String DATASET_NOME = "Orçamento por CC";
    static final String WIDGET_TABELA_NOME = "Orçado x Realizado";
    static final String WIDGET_KPI_NOME = "Variação %";

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final DatasetService datasetService;
    private final WidgetDefinitionService widgetDefinitionService;
    private final WorkspaceService workspaceService;

    @Transactional
    public OrcamentoInstallResultDTO instalarOrcamentoPadrao(String login, Long workspaceId) {
        workspaceAccessGuard.assertEscopo(login);
        workspaceService.findOwnedWorkspace(
            workspaceAccessGuard.resolve(login).usuarioId(), workspaceId);

        DatasetDTO dataset = criarDatasetSeNecessario(login);
        WidgetDefinitionDTO tabela = criarWidgetTabela(login, dataset.id());
        WidgetDefinitionDTO kpi = criarWidgetKpi(login, dataset.id());

        atualizarLayout(login, workspaceId, tabela.id(), kpi.id());

        return new OrcamentoInstallResultDTO(
            workspaceId, dataset.id(), List.of(tabela.id(), kpi.id()));
    }

    private DatasetDTO criarDatasetSeNecessario(String login) {
        return datasetService.listar(login).stream()
            .filter(d -> DATASET_NOME.equals(d.nome()))
            .findFirst()
            .map(summary -> datasetService.obter(login, summary.id()))
            .orElseGet(() -> datasetService.criar(login, new CreateDatasetRequest(
                DATASET_NOME,
                List.of(
                    new DatasetFieldSchemaDTO("competencia", DatasetFieldType.DATA, null, true, null),
                    new DatasetFieldSchemaDTO(
                        "centro_custo_id", DatasetFieldType.REFERENCIA, ReferenciaEntidade.CENTRO_CUSTO, true, null),
                    new DatasetFieldSchemaDTO("valor_orcado", DatasetFieldType.MOEDA, null, true, null)))));
    }

    private WidgetDefinitionDTO criarWidgetTabela(String login, Long datasetId) {
        String datasetRef = String.valueOf(datasetId);
        return widgetDefinitionService.listar(login).stream()
            .filter(w -> WIDGET_TABELA_NOME.equals(w.nome()))
            .findFirst()
            .orElseGet(() -> widgetDefinitionService.criar(login, new CreateWidgetDefinitionRequest(
                WIDGET_TABELA_NOME,
                "TABELA",
                List.of(
                    new WidgetSourceRef(WidgetSourceKind.DATASET, datasetRef),
                    new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")),
                null,
                Map.of("colunas", List.of("centro_custo", "orcado", "realizado", "variacao")))));
    }

    private WidgetDefinitionDTO criarWidgetKpi(String login, Long datasetId) {
        String datasetRef = String.valueOf(datasetId);
        return widgetDefinitionService.listar(login).stream()
            .filter(w -> WIDGET_KPI_NOME.equals(w.nome()))
            .findFirst()
            .orElseGet(() -> widgetDefinitionService.criar(login, new CreateWidgetDefinitionRequest(
                WIDGET_KPI_NOME,
                "KPI",
                List.of(
                    new WidgetSourceRef(WidgetSourceKind.DATASET, datasetRef),
                    new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")),
                "SE(MÉDIA(realizado)=0, 0, (SOMA(valor_orcado)-SOMA(realizado))/SOMA(realizado)*100)",
                Map.of())));
    }

    private void atualizarLayout(String login, Long workspaceId, Long tabelaId, Long kpiId) {
        SaveWorkspaceLayoutRequest layout = new SaveWorkspaceLayoutRequest(List.of(
            new WorkspaceWidgetDTO(gerarInstanceId(), 0, 12, 2, null, tabelaId, Map.of()),
            new WorkspaceWidgetDTO(gerarInstanceId(), 1, 4, 1, null, kpiId, Map.of())));
        workspaceService.salvarLayout(login, workspaceId, layout);
    }

    private String gerarInstanceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
