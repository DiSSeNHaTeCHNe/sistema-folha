package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.port.WorkspaceConsultaPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceMetadataController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceMetadataControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceConsultaPort workspaceConsultaPort;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTemplates_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/metadata/templates"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarTemplates_semEscopo_retorna403() throws Exception {
        when(workspaceConsultaPort.listarTemplatesVisiveis("user-a"))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/metadata/templates"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarTemplates_comEscopo_retorna200() throws Exception {
        when(workspaceConsultaPort.listarTemplatesVisiveis("user-a")).thenReturn(List.of(
            new TemplateCatalogItemDTO(1L, "Orçamento", TemplateTipo.DATASET, 1, 1, false, 2L, null, null)
        ));

        mockMvc.perform(get("/workspace/metadata/templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Orçamento"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarCamposSistema_retorna200() throws Exception {
        when(workspaceConsultaPort.listarCamposSistema("user-a")).thenReturn(List.of(
            new SystemFieldDescriptorDTO("FOLHA", "total_liquido", "NUMERO")
        ));

        mockMvc.perform(get("/workspace/metadata/system-fields"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fonte").value("FOLHA"))
            .andExpect(jsonPath("$[0].nome").value("total_liquido"));
    }

    @Test
    void listarCamposSistema_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/metadata/system-fields"))
            .andExpect(status().isForbidden());
    }
}
