package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.OrcamentoTemplateInstaller;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TemplateController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TemplateControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrcamentoTemplateInstaller orcamentoTemplateInstaller;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void instalarOrcamento_semAuth_retorna403() throws Exception {
        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarOrcamento_semEscopo_retorna403() throws Exception {
        when(orcamentoTemplateInstaller.instalarOrcamentoPadrao(eq("user-a"), eq(1L)))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarOrcamento_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarOrcamento_workspaceNaoEncontrado_retorna404() throws Exception {
        when(orcamentoTemplateInstaller.instalarOrcamentoPadrao(eq("user-a"), eq(99L)))
            .thenThrow(new WorkspaceNotFoundException(99L));

        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":99}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarOrcamento_valido_retorna201() throws Exception {
        when(orcamentoTemplateInstaller.instalarOrcamentoPadrao(eq("user-a"), eq(1L)))
            .thenReturn(new OrcamentoInstallResultDTO(1L, 10L, List.of(20L, 21L)));

        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.datasetId").value(10))
            .andExpect(jsonPath("$.widgetDefinitionIds.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarOrcamento_retornaWorkspaceId() throws Exception {
        when(orcamentoTemplateInstaller.instalarOrcamentoPadrao(eq("user-a"), eq(5L)))
            .thenReturn(new OrcamentoInstallResultDTO(5L, 11L, List.of(30L)));

        mockMvc.perform(post("/workspace/templates/orcamento-padrao/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.workspaceId").value(5));
    }
}
