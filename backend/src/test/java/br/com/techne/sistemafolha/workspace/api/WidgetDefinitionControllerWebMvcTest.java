package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.WidgetDefinitionService;
import br.com.techne.sistemafolha.workspace.domain.InvalidFormulaException;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinitionNotFoundException;
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

@WebMvcTest(controllers = WidgetDefinitionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WidgetDefinitionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WidgetDefinitionService widgetDefinitionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/widget-definitions"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_semEscopo_retorna403() throws Exception {
        when(widgetDefinitionService.listar("user-a")).thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/widget-definitions"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_comEscopo_retorna200() throws Exception {
        when(widgetDefinitionService.listar("user-a")).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/workspace/widget-definitions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("KPI Total"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/widget-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"tipo\":\"KPI\",\"fontes\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_formulaInvalida_retorna400ComCampoFormula() throws Exception {
        when(widgetDefinitionService.criar(eq("user-a"), any(CreateWidgetDefinitionRequest.class)))
            .thenThrow(new InvalidFormulaException(List.of("Campo inválido: x")));

        mockMvc.perform(post("/workspace/widget-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"KPI","tipo":"KPI","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}],
                    "formula":"SOMA(x)"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors[0].field").value("formula"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_valido_retorna201() throws Exception {
        when(widgetDefinitionService.criar(eq("user-a"), any(CreateWidgetDefinitionRequest.class)))
            .thenReturn(sampleDto());

        mockMvc.perform(post("/workspace/widget-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"KPI Total","tipo":"KPI","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}],
                    "formula":"SOMA(total_proventos)"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obter_naoEncontrado_retorna404() throws Exception {
        when(widgetDefinitionService.obter("user-a", 99L))
            .thenThrow(new WorkspaceWidgetDefinitionNotFoundException(99L));

        mockMvc.perform(get("/workspace/widget-definitions/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void atualizar_formulaInvalida_retorna400() throws Exception {
        when(widgetDefinitionService.atualizar(eq("user-a"), eq(5L), any(UpdateWidgetDefinitionRequest.class)))
            .thenThrow(new InvalidFormulaException(List.of("Função não permitida: EVAL")));

        mockMvc.perform(put("/workspace/widget-definitions/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"KPI","tipo":"KPI","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}],
                    "formula":"EVAL()"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("formula"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void atualizar_valido_retorna200() throws Exception {
        when(widgetDefinitionService.atualizar(eq("user-a"), eq(5L), any(UpdateWidgetDefinitionRequest.class)))
            .thenReturn(sampleDto());

        mockMvc.perform(put("/workspace/widget-definitions/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"KPI Total","tipo":"KPI","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}],
                    "formula":"SOMA(total_proventos)"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("KPI Total"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_quotaExcedida_retorna400() throws Exception {
        when(widgetDefinitionService.criar(eq("user-a"), any(CreateWidgetDefinitionRequest.class)))
            .thenThrow(new WorkspaceQuotaExceededException("Limite widgets"));

        mockMvc.perform(post("/workspace/widget-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"KPI","tipo":"KPI","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void excluir_retorna204() throws Exception {
        mockMvc.perform(delete("/workspace/widget-definitions/5"))
            .andExpect(status().isNoContent());

        verify(widgetDefinitionService).excluir("user-a", 5L);
    }

    @Test
    @WithMockUser(username = "user-b", roles = "USER")
    void obter_isolamentoUsuarios_usaLoginCorreto() throws Exception {
        when(widgetDefinitionService.obter("user-b", 5L)).thenReturn(sampleDto());

        mockMvc.perform(get("/workspace/widget-definitions/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));

        verify(widgetDefinitionService).obter("user-b", 5L);
    }

    private WidgetDefinitionDTO sampleDto() {
        return new WidgetDefinitionDTO(
            5L,
            "KPI Total",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")),
            "SOMA(total_proventos)",
            Map.of(),
            false
        );
    }
}
