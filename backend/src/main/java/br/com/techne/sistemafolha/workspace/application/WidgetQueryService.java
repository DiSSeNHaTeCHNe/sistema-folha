package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDataDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetQueryParams;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import br.com.techne.sistemafolha.workspace.domain.InvalidFormulaException;
import br.com.techne.sistemafolha.workspace.domain.formula.EvaluationContext;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import br.com.techne.sistemafolha.workspace.domain.formula.TypedValue;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceWidgetDefinitionRepository;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort.OrcamentoCentroCustoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WidgetQueryService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String PREVIEW_INSTANCE_ID = "preview";
    private static final Set<String> VALID_TIPOS = Set.of(
        "KPI", "TABELA", "GRAFICO_LINHA", "GRAFICO_BARRA"
    );

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final WorkspaceService workspaceService;
    private final WorkspaceWidgetDefinitionRepository widgetDefinitionRepository;
    private final WorkspaceDatasetRowRepository rowRepository;
    private final DatasetService datasetService;
    private final WidgetDefinitionService widgetDefinitionService;
    private final FormulaEngine formulaEngine;
    private final OrcamentoConsultaPort orcamentoConsultaPort;
    private final FolhaConsultaPort folhaConsultaPort;

    @Transactional(readOnly = true)
    public WorkspaceWidgetDataDTO obterDados(
            String login, Long workspaceId, String instanceId, WorkspaceWidgetQueryParams params) {
        workspaceAccessGuard.assertEscopo(login);
        WorkspaceAccessGuard.ResolvedWorkspaceAccess access = workspaceAccessGuard.resolve(login);
        Workspace workspace = workspaceService.findOwnedWorkspace(access.usuarioId(), workspaceId);
        WorkspaceWidgetPayload layoutWidget = workspace.getWidgets().stream()
            .filter(w -> instanceId.equals(w.instanceId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Widget não encontrado no layout: " + instanceId));

        if (layoutWidget.userWidgetDefinitionId() == null) {
            return WorkspaceWidgetDataDTO.semDados(
                instanceId, null, layoutWidget.widgetId(), "CATALOGO", resolverCompetenciaLabel(params));
        }

        WorkspaceWidgetDefinition definition = widgetDefinitionRepository
            .findByUsuarioIdAndId(access.usuarioId(), layoutWidget.userWidgetDefinitionId())
            .orElseThrow(() -> new IllegalArgumentException("Definição de widget não encontrada"));

        if (Boolean.TRUE.equals(definition.getInvalido())) {
            return new WorkspaceWidgetDataDTO(
                instanceId,
                definition.getId(),
                null,
                definition.getTipo(),
                true,
                true,
                resolverCompetenciaLabel(params),
                Map.of(),
                List.of());
        }

        YearMonth competencia = resolverCompetencia(params);
        String competenciaLabel = competencia.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        if (semEscopoSistema(access.contexto())) {
            return WorkspaceWidgetDataDTO.semDados(
                instanceId, definition.getId(), null, definition.getTipo(), competenciaLabel);
        }

        return switch (definition.getTipo()) {
            case "KPI" -> resolverKpi(access, layoutWidget, definition, competencia, competenciaLabel);
            case "TABELA" -> resolverTabela(access, layoutWidget, definition, competencia, competenciaLabel);
            case "GRAFICO_LINHA", "GRAFICO_BARRA" -> resolverGrafico(
                access, layoutWidget, definition, competencia, competenciaLabel);
            default -> WorkspaceWidgetDataDTO.semDados(
                instanceId, definition.getId(), null, definition.getTipo(), competenciaLabel);
        };
    }

    @Transactional(readOnly = true)
    public WorkspaceWidgetDataDTO preview(String login, CreateWidgetDefinitionRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        WorkspaceAccessGuard.ResolvedWorkspaceAccess access = workspaceAccessGuard.resolve(login);
        validarPreviewRequest(login, request);

        WorkspaceWidgetDefinition definition = toEphemeralDefinition(request);
        WorkspaceWidgetPayload layoutWidget = new WorkspaceWidgetPayload(
            PREVIEW_INSTANCE_ID, 0, 4, 1, null, null, Map.of());

        YearMonth competencia = resolverCompetencia(null);
        String competenciaLabel = competencia.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        if (semEscopoSistema(access.contexto())) {
            return WorkspaceWidgetDataDTO.semDados(
                PREVIEW_INSTANCE_ID, null, null, definition.getTipo(), competenciaLabel);
        }

        return switch (definition.getTipo()) {
            case "KPI" -> resolverKpi(access, layoutWidget, definition, competencia, competenciaLabel);
            case "TABELA" -> resolverTabela(access, layoutWidget, definition, competencia, competenciaLabel);
            case "GRAFICO_LINHA", "GRAFICO_BARRA" -> resolverGrafico(
                access, layoutWidget, definition, competencia, competenciaLabel);
            default -> WorkspaceWidgetDataDTO.semDados(
                PREVIEW_INSTANCE_ID, null, null, definition.getTipo(), competenciaLabel);
        };
    }

    private void validarPreviewRequest(String login, CreateWidgetDefinitionRequest request) {
        if (request.tipo() == null || !VALID_TIPOS.contains(request.tipo())) {
            throw new IllegalArgumentException("tipo: inválido — use KPI, TABELA, GRAFICO_LINHA ou GRAFICO_BARRA");
        }
        if (request.formula() != null && !request.formula().isBlank()) {
            var fields = widgetDefinitionService.buildAvailableFields(login, request.fontes());
            FormulaValidationResult result = formulaEngine.validate(request.formula(), fields);
            if (!result.valid()) {
                throw new InvalidFormulaException(result.errors());
            }
        }
    }

    private WorkspaceWidgetDefinition toEphemeralDefinition(CreateWidgetDefinitionRequest request) {
        WorkspaceWidgetDefinition definition = new WorkspaceWidgetDefinition();
        definition.setNome(request.nome().trim());
        definition.setTipo(request.tipo());
        definition.setFontes(new ArrayList<>(request.fontes()));
        definition.setFormula(blankToNull(request.formula()));
        definition.setConfig(request.config() != null ? new HashMap<>(request.config()) : new HashMap<>());
        definition.setInvalido(false);
        return definition;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private WorkspaceWidgetDataDTO resolverKpi(
            WorkspaceAccessGuard.ResolvedWorkspaceAccess access,
            WorkspaceWidgetPayload layoutWidget,
            WorkspaceWidgetDefinition definition,
            YearMonth competencia,
            String competenciaLabel) {
        EvaluationContext ctx = buildEvaluationContext(access, definition, competencia);
        if (definition.getFormula() == null || definition.getFormula().isBlank()) {
            return WorkspaceWidgetDataDTO.semDados(
                layoutWidget.instanceId(), definition.getId(), null, definition.getTipo(), competenciaLabel);
        }
        TypedValue result = formulaEngine.evaluate(definition.getFormula(), ctx);
        Map<String, String> valores = Map.of("valor", formatMoney(result.asNumber()));
        return new WorkspaceWidgetDataDTO(
            layoutWidget.instanceId(),
            definition.getId(),
            null,
            definition.getTipo(),
            false,
            false,
            competenciaLabel,
            valores,
            List.of());
    }

    private WorkspaceWidgetDataDTO resolverGrafico(
            WorkspaceAccessGuard.ResolvedWorkspaceAccess access,
            WorkspaceWidgetPayload layoutWidget,
            WorkspaceWidgetDefinition definition,
            YearMonth competencia,
            String competenciaLabel) {
        List<Map<String, String>> linhas = new ArrayList<>();
        List<OrcamentoCentroCustoDTO> realizados = orcamentoConsultaPort.obterRealizadoPorCentroCusto(
            access.contexto(), competencia);

        for (OrcamentoCentroCustoDTO realizado : realizados) {
            Map<String, String> linha = new LinkedHashMap<>();
            linha.put("label", realizado.centroCustoDescricao());
            linha.put("valor", formatMoney(realizado.realizado()));
            linhas.add(linha);
        }

        boolean semDados = linhas.isEmpty();
        return new WorkspaceWidgetDataDTO(
            layoutWidget.instanceId(),
            definition.getId(),
            null,
            definition.getTipo(),
            semDados,
            false,
            competenciaLabel,
            Map.of(),
            linhas);
    }

    private WorkspaceWidgetDataDTO resolverTabela(
            WorkspaceAccessGuard.ResolvedWorkspaceAccess access,
            WorkspaceWidgetPayload layoutWidget,
            WorkspaceWidgetDefinition definition,
            YearMonth competencia,
            String competenciaLabel) {
        List<Map<String, String>> linhas = new ArrayList<>();
        Map<Long, BigDecimal> orcadoPorCc = carregarOrcadoPorCentroCusto(access.usuarioId(), definition);
        List<OrcamentoCentroCustoDTO> realizados = orcamentoConsultaPort.obterRealizadoPorCentroCusto(
            access.contexto(), competencia);

        for (OrcamentoCentroCustoDTO realizado : realizados) {
            BigDecimal orcado = orcadoPorCc.getOrDefault(realizado.centroCustoId(), BigDecimal.ZERO);
            BigDecimal variacao = orcado.subtract(realizado.realizado());
            Map<String, String> linha = new LinkedHashMap<>();
            linha.put("centro_custo", realizado.centroCustoDescricao());
            linha.put("orcado", formatMoney(orcado));
            linha.put("realizado", formatMoney(realizado.realizado()));
            linha.put("variacao", formatMoney(variacao));
            linhas.add(linha);
        }

        boolean semDados = linhas.isEmpty();
        return new WorkspaceWidgetDataDTO(
            layoutWidget.instanceId(),
            definition.getId(),
            null,
            definition.getTipo(),
            semDados,
            false,
            competenciaLabel,
            Map.of(),
            linhas);
    }

    private Map<Long, BigDecimal> carregarOrcadoPorCentroCusto(Long usuarioId, WorkspaceWidgetDefinition definition) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (WidgetSourceRef fonte : definition.getFontes()) {
            if (fonte.kind() != WidgetSourceKind.DATASET) {
                continue;
            }
            Long datasetId = Long.parseLong(fonte.ref());
            WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);
            Optional<DatasetFieldSchema> ccField = dataset.getSchema().stream()
                .filter(f -> f.tipo() == DatasetFieldType.REFERENCIA)
                .findFirst();
            Optional<DatasetFieldSchema> valorField = dataset.getSchema().stream()
                .filter(f -> f.tipo() == DatasetFieldType.MOEDA)
                .findFirst();
            if (ccField.isEmpty() || valorField.isEmpty()) {
                continue;
            }
            List<WorkspaceDatasetRow> rows = rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(datasetId);
            for (WorkspaceDatasetRow row : rows) {
                Object ccId = row.getValores().get(ccField.get().nome());
                Object valor = row.getValores().get(valorField.get().nome());
                if (ccId == null || valor == null) {
                    continue;
                }
                Long centroId = ((Number) ccId).longValue();
                BigDecimal amount = new BigDecimal(valor.toString());
                totals.merge(centroId, amount, BigDecimal::add);
            }
        }
        return totals;
    }

    private EvaluationContext buildEvaluationContext(
            WorkspaceAccessGuard.ResolvedWorkspaceAccess access,
            WorkspaceWidgetDefinition definition,
            YearMonth competencia) {
        EvaluationContext.Builder builder = EvaluationContext.builder();
        for (WidgetSourceRef fonte : definition.getFontes()) {
            if (fonte.kind() == WidgetSourceKind.DATASET) {
                addDatasetSeries(builder, access.usuarioId(), Long.parseLong(fonte.ref()));
            } else if (fonte.kind() == WidgetSourceKind.SISTEMA) {
                addSystemSeries(builder, fonte.ref(), access, competencia);
            }
        }
        return builder.build();
    }

    private void addDatasetSeries(EvaluationContext.Builder builder, Long usuarioId, Long datasetId) {
        WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);
        List<WorkspaceDatasetRow> rows = rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(datasetId);
        for (DatasetFieldSchema field : dataset.getSchema()) {
            if (field.tipo() != DatasetFieldType.MOEDA && field.tipo() != DatasetFieldType.NUMERO) {
                continue;
            }
            List<BigDecimal> values = rows.stream()
                .map(r -> r.getValores().get(field.nome()))
                .filter(v -> v != null)
                .map(v -> new BigDecimal(v.toString()))
                .collect(Collectors.toList());
            builder.putSeries(field.nome(), values);
        }
    }

    private void addSystemSeries(
            EvaluationContext.Builder builder,
            String ref,
            WorkspaceAccessGuard.ResolvedWorkspaceAccess access,
            YearMonth competencia) {
        if (!"ORCAMENTO".equalsIgnoreCase(ref)) {
            return;
        }
        List<OrcamentoCentroCustoDTO> realizados = orcamentoConsultaPort.obterRealizadoPorCentroCusto(
            access.contexto(), competencia);
        List<BigDecimal> valores = realizados.stream().map(OrcamentoCentroCustoDTO::realizado).toList();
        builder.putSeries("realizado", valores);
        builder.putSeries("orcado", List.of());
    }

    private boolean semEscopoSistema(AccessContextDTO ctx) {
        if (ctx.acessoTotal()) {
            return false;
        }
        if (ctx.motivoNegacao() != null) {
            return true;
        }
        Set<Long> centros = ctx.centrosCustoIds();
        return centros == null || centros.isEmpty();
    }

    private YearMonth resolverCompetencia(WorkspaceWidgetQueryParams params) {
        if (params != null && params.competencia() != null && !params.competencia().isBlank()) {
            try {
                return YearMonth.parse(params.competencia(), DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("competencia deve estar no formato yyyy-MM");
            }
        }
        Optional<FolhaResumoSnapshot> resumo = folhaConsultaPort.findResumoMaisRecente();
        return resumo.map(s -> YearMonth.from(s.competenciaInicio())).orElse(YearMonth.now());
    }

    private String resolverCompetenciaLabel(WorkspaceWidgetQueryParams params) {
        return resolverCompetencia(params).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    static String formatMoney(BigDecimal value) {
        if (value == null) {
            return NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.ZERO);
        }
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }
}
