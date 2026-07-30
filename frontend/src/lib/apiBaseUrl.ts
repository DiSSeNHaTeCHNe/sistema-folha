const DEFAULT_API_BASE_URL = 'http://localhost:8083/api';

/** Single source of truth for axios client and MSW handlers. */
export function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_URL || DEFAULT_API_BASE_URL;
}

export const API_BASE_URL = getApiBaseUrl();
