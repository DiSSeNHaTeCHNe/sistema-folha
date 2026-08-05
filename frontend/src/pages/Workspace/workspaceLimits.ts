/**
 * Mirror of backend WorkspaceLimits.java — sync on change.
 * @see backend/src/main/java/br/com/techne/sistemafolha/workspace/domain/WorkspaceLimits.java
 */
export const WORKSPACE_LIMITS = {
  MAX_DATASETS_PER_USER: 20,
  MAX_ROWS_PER_DATASET: 500,
  MAX_FIELDS_PER_DATASET: 30,
  MAX_USER_WIDGET_DEFINITIONS: 50,
  MAX_WORKSPACES_PER_USER: 10,
  MAX_WIDGETS_PER_WORKSPACE: 30,
  MAX_PENDING_IA_PROPOSALS: 5,
  IA_PROPOSAL_TTL_HOURS: 72,
  MAX_ENTITY_NAME_LENGTH: 120,
} as const;

export type WorkspaceLimits = typeof WORKSPACE_LIMITS;
