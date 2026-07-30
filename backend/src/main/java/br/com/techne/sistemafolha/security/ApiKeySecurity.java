package br.com.techne.sistemafolha.security;

/**
 * Constantes de segurança para autenticação via API Key (marker read-only em runtime).
 */
public final class ApiKeySecurity {

    public static final String CHAVE_PREFIX = "sf_live_";
    public static final String ROLE_API_KEY_READONLY = "ROLE_API_KEY_READONLY";

    private ApiKeySecurity() {
    }
}
