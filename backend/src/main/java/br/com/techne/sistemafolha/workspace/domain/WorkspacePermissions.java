package br.com.techne.sistemafolha.workspace.domain;

/**
 * Permissões dedicadas do domínio workspace.
 *
 * <p>{@link #WORKSPACE_IA_CRIAR} — string canônica para escrita via IA/MCP (WKS-24/25).
 * Concedida via tabela {@code usuario_permissoes} ou escopo de API Key do usuário titular.
 * Seed opcional apenas em ambientes de desenvolvimento.
 */
public final class WorkspacePermissions {

    public static final String WORKSPACE_IA_CRIAR = "WORKSPACE_IA_CRIAR";

    private WorkspacePermissions() {
    }
}
