package br.com.techne.sistemafolha.workspace.domain;

public final class WorkspaceLimits {

    public static final int MAX_DATASETS_PER_USER = 20;
    public static final int MAX_ROWS_PER_DATASET = 500;
    public static final int MAX_FIELDS_PER_DATASET = 30;
    public static final int MAX_USER_WIDGET_DEFINITIONS = 50;
    public static final int MAX_WORKSPACES_PER_USER = 10;
    public static final int MAX_WIDGETS_PER_WORKSPACE = 30;
    public static final int MAX_PENDING_IA_PROPOSALS = 5;
    public static final int IA_PROPOSAL_TTL_HOURS = 72;
    public static final int MAX_ENTITY_NAME_LENGTH = 120;

    private WorkspaceLimits() {
    }
}
