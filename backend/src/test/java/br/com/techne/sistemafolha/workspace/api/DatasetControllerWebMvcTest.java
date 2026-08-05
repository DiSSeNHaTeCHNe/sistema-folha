package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.DatasetAuditService;
import br.com.techne.sistemafolha.workspace.application.DatasetRowService;
import br.com.techne.sistemafolha.workspace.application.DatasetService;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowValidationException;
import br.com.techne.sistemafolha.workspace.domain.FieldValidationError;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
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

import java.time.LocalDateTime;
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

@WebMvcTest(controllers = DatasetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DatasetControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatasetService datasetService;

    @MockBean
    private DatasetRowService datasetRowService;

    @MockBean
    private DatasetAuditService datasetAuditService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/datasets"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_semEscopo_retorna403() throws Exception {
        when(datasetService.listar("user-a")).thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/datasets"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listar_comEscopo_retorna200() throws Exception {
        when(datasetService.listar("user-a")).thenReturn(List.of(
            new DatasetSummaryDTO(
                1L, "Planilha", 1, 2L, 3,
                LocalDateTime.parse("2026-08-01T14:30:00"), true, 2)));

        mockMvc.perform(get("/workspace/datasets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Planilha"))
            .andExpect(jsonPath("$[0].dataAtualizacao").exists())
            .andExpect(jsonPath("$[0].publicado").value(true))
            .andExpect(jsonPath("$[0].templateVersaoPublicada").value(2));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/datasets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"campos\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_quotaExcedida_retorna400() throws Exception {
        when(datasetService.criar(eq("user-a"), any(CreateDatasetRequest.class)))
            .thenThrow(new WorkspaceQuotaExceededException("Limite atingido"));

        mockMvc.perform(post("/workspace/datasets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo\",\"campos\":[{\"nome\":\"a\",\"tipo\":\"TEXTO\"}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_valido_retorna201() throws Exception {
        when(datasetService.criar(eq("user-a"), any(CreateDatasetRequest.class)))
            .thenReturn(new DatasetDTO(1L, "Novo", List.of(
                new DatasetFieldSchemaDTO("a", DatasetFieldType.TEXTO, null, false)), 1, 0L));

        mockMvc.perform(post("/workspace/datasets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Novo\",\"campos\":[{\"nome\":\"a\",\"tipo\":\"TEXTO\"}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obter_datasetInexistente_retorna404() throws Exception {
        when(datasetService.obter("user-a", 99L)).thenThrow(new WorkspaceDatasetNotFoundException(99L));

        mockMvc.perform(get("/workspace/datasets/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void atualizarSchema_versaoStale_retorna409() throws Exception {
        when(datasetService.atualizarSchema(eq("user-a"), eq(1L), any(UpdateDatasetSchemaRequest.class)))
            .thenThrow(new WorkspaceDatasetConflictException("Versão desatualizada"));

        mockMvc.perform(put("/workspace/datasets/1/schema")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campos\":[{\"nome\":\"a\",\"tipo\":\"TEXTO\"}],\"schemaVersion\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void atualizarSchema_remocaoSemConfirmacao_retorna409() throws Exception {
        when(datasetService.atualizarSchema(eq("user-a"), eq(1L), any(UpdateDatasetSchemaRequest.class)))
            .thenThrow(new WorkspaceDatasetConflictException("confirme a remoção"));

        mockMvc.perform(put("/workspace/datasets/1/schema")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campos\":[{\"nome\":\"b\",\"tipo\":\"TEXTO\"}],\"schemaVersion\":1,\"confirmarRemocao\":false}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void adicionarLinha_tipoInvalido_retorna400ComErrosPorCampo() throws Exception {
        when(datasetRowService.adicionarLinha(eq("user-a"), eq(1L), any(DatasetRowRequest.class)))
            .thenThrow(new DatasetRowValidationException(List.of(
                new FieldValidationError("quantidade", "Valor incompatível com campo numérico"))));

        mockMvc.perform(post("/workspace/datasets/1/rows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valores\":{\"quantidade\":\"abc\"}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors[0].field").value("quantidade"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void adicionarLinha_valida_retorna201() throws Exception {
        when(datasetRowService.adicionarLinha(eq("user-a"), eq(1L), any(DatasetRowRequest.class)))
            .thenReturn(new DatasetRowDTO(10L, 1L, Map.of("quantidade", 5), 0));

        mockMvc.perform(post("/workspace/datasets/1/rows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valores\":{\"quantidade\":5}}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarLinhas_retorna200() throws Exception {
        when(datasetRowService.listarLinhas("user-a", 1L))
            .thenReturn(List.of(new DatasetRowDTO(10L, 1L, Map.of("quantidade", 5), 0)));

        mockMvc.perform(get("/workspace/datasets/1/rows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void excluirDataset_retorna204() throws Exception {
        mockMvc.perform(delete("/workspace/datasets/1"))
            .andExpect(status().isNoContent());

        verify(datasetService).excluir("user-a", 1L);
    }

    @Test
    @WithMockUser(username = "user-b", roles = "USER")
    void obter_isolamentoUsuarios_usaLoginCorreto() throws Exception {
        when(datasetService.obter("user-b", 1L))
            .thenReturn(new DatasetDTO(1L, "Privado", List.of(), 1, 0L));

        mockMvc.perform(get("/workspace/datasets/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Privado"));

        verify(datasetService).obter("user-b", 1L);
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void removerLinha_retorna204() throws Exception {
        mockMvc.perform(delete("/workspace/datasets/1/rows/10"))
            .andExpect(status().isNoContent());

        verify(datasetRowService).removerLinha("user-a", 1L, 10L);
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarAuditoriaDataset_retornaTimelineCronologica() throws Exception {
        when(datasetAuditService.listarHistoricoDataset("user-a", 1L)).thenReturn(List.of(
            new DatasetAuditTimelineEntryDTO(
                10L, DatasetRowAuditAction.UPDATE, 5L,
                java.time.LocalDateTime.parse("2026-01-01T11:00:00"), "Campos alterados: valor"),
            new DatasetAuditTimelineEntryDTO(
                10L, DatasetRowAuditAction.CREATE, 5L,
                java.time.LocalDateTime.parse("2026-01-01T10:00:00"), "Linha criada")));

        mockMvc.perform(get("/workspace/datasets/1/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].acao").value("UPDATE"))
            .andExpect(jsonPath("$[1].acao").value("CREATE"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarAuditoriaDataset_inexistente_retorna404() throws Exception {
        when(datasetAuditService.listarHistoricoDataset("user-a", 99L))
            .thenThrow(new WorkspaceDatasetNotFoundException(99L));

        mockMvc.perform(get("/workspace/datasets/99/audit"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarAuditoriaDataset_semEscopo_retorna403() throws Exception {
        when(datasetAuditService.listarHistoricoDataset("user-a", 1L))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/datasets/1/audit"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarAuditoriaLinha_retornaHistoricoCronologico() throws Exception {
        when(datasetRowService.obterLinha("user-a", 1L, 10L))
            .thenReturn(new DatasetRowDTO(10L, 1L, Map.of("valor", 5), 0));
        when(datasetAuditService.listarHistorico(10L)).thenReturn(List.of(
            new DatasetRowAuditEntryDTO(1L, 10L, 5L, DatasetRowAuditAction.CREATE, null,
                Map.of("valor", 5), java.time.LocalDateTime.parse("2026-01-01T10:00:00")),
            new DatasetRowAuditEntryDTO(2L, 10L, 5L, DatasetRowAuditAction.UPDATE,
                Map.of("valor", 5), Map.of("valor", 10), java.time.LocalDateTime.parse("2026-01-01T11:00:00"))));

        mockMvc.perform(get("/workspace/datasets/1/rows/10/audit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].acao").value("CREATE"))
            .andExpect(jsonPath("$[1].acao").value("UPDATE"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarAuditoriaLinha_inexistente_retorna404() throws Exception {
        when(datasetRowService.obterLinha("user-a", 1L, 99L))
            .thenThrow(new br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowNotFoundException(1L, 99L));

        mockMvc.perform(get("/workspace/datasets/1/rows/99/audit"))
            .andExpect(status().isNotFound());
    }
}
