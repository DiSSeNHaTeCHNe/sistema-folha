package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.workspace.application.WorkspaceService;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNameConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private br.com.techne.sistemafolha.security.JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/workspaces"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_semEscopo_retorna403() throws Exception {
        when(workspaceService.listar("user-a")).thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/workspaces"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_comEscopo_retorna200() throws Exception {
        when(workspaceService.listar("user-a")).thenReturn(List.of(
            new WorkspaceSummaryDTO(1L, "Financeiro", 2)));

        mockMvc.perform(get("/workspace/workspaces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Financeiro"))
            .andExpect(jsonPath("$[0].totalWidgets").value(2));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_nomeDuplicado_retorna409() throws Exception {
        when(workspaceService.criar(eq("user-a"), any(CreateWorkspaceRequest.class)))
            .thenThrow(new WorkspaceNameConflictException("Financeiro"));

        mockMvc.perform(post("/workspace/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Financeiro\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_quotaExcedida_retorna400() throws Exception {
        when(workspaceService.criar(eq("user-a"), any(CreateWorkspaceRequest.class)))
            .thenThrow(new WorkspaceQuotaExceededException("Limite workspaces"));

        mockMvc.perform(post("/workspace/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_valido_retorna201() throws Exception {
        when(workspaceService.criar(eq("user-a"), any(CreateWorkspaceRequest.class)))
            .thenReturn(new WorkspaceDTO(10L, "Financeiro", List.of()));

        mockMvc.perform(post("/workspace/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Financeiro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.nome").value("Financeiro"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obter_naoEncontrado_retorna404() throws Exception {
        when(workspaceService.obter("user-a", 99L)).thenThrow(new WorkspaceNotFoundException(99L));

        mockMvc.perform(get("/workspace/workspaces/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obter_valido_retorna200() throws Exception {
        when(workspaceService.obter("user-a", 10L)).thenReturn(sampleDto());

        mockMvc.perform(get("/workspace/workspaces/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.widgets[0].instanceId").value("abc12345"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void salvarLayout_layoutInvalido_retorna400() throws Exception {
        when(workspaceService.salvarLayout(eq("user-a"), eq(10L), any(SaveWorkspaceLayoutRequest.class)))
            .thenThrow(new IllegalArgumentException("Limite de 30 widgets atingido"));

        mockMvc.perform(put("/workspace/workspaces/10/layout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"widgets":[{"instanceId":"a","ordem":0,"colSpan":3,"rowSpan":1}]}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void salvarLayout_valido_retorna200() throws Exception {
        when(workspaceService.salvarLayout(eq("user-a"), eq(10L), any(SaveWorkspaceLayoutRequest.class)))
            .thenReturn(sampleDto());

        mockMvc.perform(put("/workspace/workspaces/10/layout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"widgets":[{"instanceId":"abc12345","ordem":0,"colSpan":3,"rowSpan":1,
                    "userWidgetDefinitionId":1}]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.widgets[0].userWidgetDefinitionId").value(1));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void excluir_valido_retorna204() throws Exception {
        mockMvc.perform(delete("/workspace/workspaces/10"))
            .andExpect(status().isNoContent());

        verify(workspaceService).excluir("user-a", 10L);
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void excluir_naoEncontrado_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new WorkspaceNotFoundException(10L))
            .when(workspaceService).excluir("user-a", 10L);

        mockMvc.perform(delete("/workspace/workspaces/10"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    private WorkspaceDTO sampleDto() {
        return new WorkspaceDTO(10L, "Financeiro", List.of(
            new WorkspaceWidgetDTO("abc12345", 0, 3, 1, null, 1L, Map.of())));
    }
}
