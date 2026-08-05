package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import org.springframework.stereotype.Component;

@Component
public class DatasetQuotaPolicy {

    public boolean canCreateDataset(long currentCount) {
        return currentCount < WorkspaceLimits.MAX_DATASETS_PER_USER;
    }

    public boolean canAddRow(long currentRowCount) {
        return currentRowCount < WorkspaceLimits.MAX_ROWS_PER_DATASET;
    }

    public boolean canAddField(int currentFieldCount) {
        return currentFieldCount < WorkspaceLimits.MAX_FIELDS_PER_DATASET;
    }

    public boolean canCreateWidgetDefinition(long currentCount) {
        return currentCount < WorkspaceLimits.MAX_USER_WIDGET_DEFINITIONS;
    }

    public boolean canCreateWorkspace(long currentCount) {
        return currentCount < WorkspaceLimits.MAX_WORKSPACES_PER_USER;
    }

    public boolean canAddWidgetToWorkspace(int currentWidgetCount) {
        return currentWidgetCount < WorkspaceLimits.MAX_WIDGETS_PER_WORKSPACE;
    }

    public String datasetQuotaMessage(long currentCount) {
        return quotaMessage("datasets", currentCount, WorkspaceLimits.MAX_DATASETS_PER_USER);
    }

    public String rowQuotaMessage(long currentCount) {
        return quotaMessage("linhas", currentCount, WorkspaceLimits.MAX_ROWS_PER_DATASET);
    }

    public String fieldQuotaMessage(int currentCount) {
        return quotaMessage("campos", currentCount, WorkspaceLimits.MAX_FIELDS_PER_DATASET);
    }

    public String widgetDefinitionQuotaMessage(long currentCount) {
        return quotaMessage("widgets definidos", currentCount, WorkspaceLimits.MAX_USER_WIDGET_DEFINITIONS);
    }

    public String workspaceQuotaMessage(long currentCount) {
        return quotaMessage("workspaces", currentCount, WorkspaceLimits.MAX_WORKSPACES_PER_USER);
    }

    public String workspaceWidgetQuotaMessage(int currentCount) {
        return quotaMessage("widgets no workspace", currentCount, WorkspaceLimits.MAX_WIDGETS_PER_WORKSPACE);
    }

    private String quotaMessage(String resource, long current, int limit) {
        return String.format("Limite de %d %s atingido (atual: %d)", limit, resource, current);
    }
}
