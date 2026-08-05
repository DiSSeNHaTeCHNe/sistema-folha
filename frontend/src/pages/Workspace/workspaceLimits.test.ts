import { describe, expect, it } from 'vitest';
import { WORKSPACE_LIMITS } from './workspaceLimits';

/**
 * Expected values from WorkspaceLimits.java (v1 baseline).
 * Update this table when backend limits change.
 */
const JAVA_LIMITS = {
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

describe('workspaceLimits', () => {
  it('mirrors all WorkspaceLimits.java constants (WKS2-04)', () => {
    expect(WORKSPACE_LIMITS).toEqual(JAVA_LIMITS);
  });

  it('exposes dataset quota limit for hub display', () => {
    expect(WORKSPACE_LIMITS.MAX_DATASETS_PER_USER).toBe(20);
  });

  it('exposes row quota limit for dataset editor display (WKS2-14)', () => {
    expect(WORKSPACE_LIMITS.MAX_ROWS_PER_DATASET).toBe(500);
  });
});
