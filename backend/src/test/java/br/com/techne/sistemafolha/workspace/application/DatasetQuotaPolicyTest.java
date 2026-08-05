package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetQuotaPolicyTest {

    private DatasetQuotaPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DatasetQuotaPolicy();
    }

    @Test
    void canCreateDataset_underLimit_allows() {
        assertTrue(policy.canCreateDataset(WorkspaceLimits.MAX_DATASETS_PER_USER - 1));
    }

    @Test
    void canCreateDataset_atLimit_blocks() {
        assertFalse(policy.canCreateDataset(WorkspaceLimits.MAX_DATASETS_PER_USER));
    }

    @Test
    void canAddRow_underLimit_allows() {
        assertTrue(policy.canAddRow(WorkspaceLimits.MAX_ROWS_PER_DATASET - 1));
    }

    @Test
    void canAddRow_atLimit_blocks() {
        assertFalse(policy.canAddRow(WorkspaceLimits.MAX_ROWS_PER_DATASET));
    }

    @Test
    void canAddField_underLimit_allows() {
        assertTrue(policy.canAddField(WorkspaceLimits.MAX_FIELDS_PER_DATASET - 1));
    }

    @Test
    void canAddField_atLimit_blocks() {
        assertFalse(policy.canAddField(WorkspaceLimits.MAX_FIELDS_PER_DATASET));
    }

    @Test
    void canCreateWidgetDefinition_underLimit_allows() {
        assertTrue(policy.canCreateWidgetDefinition(WorkspaceLimits.MAX_USER_WIDGET_DEFINITIONS - 1));
    }

    @Test
    void canCreateWidgetDefinition_atLimit_blocks() {
        assertFalse(policy.canCreateWidgetDefinition(WorkspaceLimits.MAX_USER_WIDGET_DEFINITIONS));
    }

    @Test
    void canCreateWorkspace_underLimit_allows() {
        assertTrue(policy.canCreateWorkspace(WorkspaceLimits.MAX_WORKSPACES_PER_USER - 1));
    }

    @Test
    void canCreateWorkspace_atLimit_blocks() {
        assertFalse(policy.canCreateWorkspace(WorkspaceLimits.MAX_WORKSPACES_PER_USER));
    }

    @Test
    void canAddWidgetToWorkspace_underLimit_allows() {
        assertTrue(policy.canAddWidgetToWorkspace(WorkspaceLimits.MAX_WIDGETS_PER_WORKSPACE - 1));
    }

    @Test
    void canAddWidgetToWorkspace_atLimit_blocks() {
        assertFalse(policy.canAddWidgetToWorkspace(WorkspaceLimits.MAX_WIDGETS_PER_WORKSPACE));
    }

    @Test
    void datasetQuotaMessage_contemLimiteSpecAncorado() {
        long current = WorkspaceLimits.MAX_DATASETS_PER_USER;
        String message = policy.datasetQuotaMessage(current);

        assertEquals(
            String.format("Limite de %d datasets atingido (atual: %d)",
                WorkspaceLimits.MAX_DATASETS_PER_USER, current),
            message);
        assertTrue(message.startsWith("Limite de 20 datasets"));
    }

    @Test
    void rowQuotaMessage_contemLimiteSpecAncorado() {
        long current = WorkspaceLimits.MAX_ROWS_PER_DATASET;
        String message = policy.rowQuotaMessage(current);

        assertTrue(message.contains(
            String.format("Limite de %d linhas", WorkspaceLimits.MAX_ROWS_PER_DATASET)));
    }
}
