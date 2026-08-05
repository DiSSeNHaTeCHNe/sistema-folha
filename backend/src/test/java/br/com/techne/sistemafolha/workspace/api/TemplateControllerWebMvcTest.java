package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.OrcamentoTemplateInstaller;
import br.com.techne.sistemafolha.workspace.application.TemplateInstallService;
import br.com.techne.sistemafolha.workspace.application.TemplatePublishService;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallationNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateNotFoundException;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private TemplatePublishService templatePublishService;

    @MockBean
    private TemplateInstallService templateInstallService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarVersoes_retornaListaOrdenadaDesc() throws Exception {
        when(templatePublishService.listarVersoes("user-a", 3L)).thenReturn(List.of(
            new TemplateVersionSummaryDTO(
                2,
                LocalDateTime.parse("2026-02-01T10:00:00"),
                new TemplateStructureResumoDTO(List.of("valor"), List.of(), List.of())),
            new TemplateVersionSummaryDTO(
                1,
                LocalDateTime.parse("2026-01-01T10:00:00"),
                new TemplateStructureResumoDTO(List.of("qtd"), List.of(), List.of()))));

        mockMvc.perform(get("/workspace/templates/3/versions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].versao").value(2))
            .andExpect(jsonPath("$[1].versao").value(1))
            .andExpect(jsonPath("$[0].estruturaResumo.campos[0]").value("valor"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarVersoes_templateInexistente_retorna404() throws Exception {
        when(templatePublishService.listarVersoes("user-a", 99L))
            .thenThrow(new WorkspaceTemplateNotFoundException(99L));

        mockMvc.perform(get("/workspace/templates/99/versions"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarVersoes_semEscopo_retorna403() throws Exception {
        when(templatePublishService.listarVersoes("user-a", 3L))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/templates/3/versions"))
            .andExpect(status().isForbidden());
    }

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
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void publicar_semAuthBodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/templates/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void publicar_datasetValido_retorna201() throws Exception {
        when(templatePublishService.publicar(eq("user-a"), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new TemplateDTO(1L, "Vendas", TemplateTipo.DATASET, 1, "hash", LocalDateTime.now(), 10L, true));

        mockMvc.perform(post("/workspace/templates/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"datasetId\":5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.novaVersaoCriada").value(true));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void publicar_semEscopo_retorna403() throws Exception {
        when(templatePublishService.publicar(eq("user-a"), org.mockito.ArgumentMatchers.any()))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(post("/workspace/templates/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"datasetId\":5}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void publicar_datasetInexistente_retorna404() throws Exception {
        when(templatePublishService.publicar(eq("user-a"), org.mockito.ArgumentMatchers.any()))
            .thenThrow(new WorkspaceDatasetNotFoundException(99L));

        mockMvc.perform(post("/workspace/templates/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"datasetId\":99}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarCatalogo_incluiOrcamentoNativo() throws Exception {
        when(templatePublishService.listarCatalogo("user-a")).thenReturn(List.of(
            new TemplateCatalogItemDTO(
                TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID,
                "Orçamento por CC",
                TemplateTipo.PACOTE,
                1, 1, false, 0L, null, null)));

        mockMvc.perform(get("/workspace/templates/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(0))
            .andExpect(jsonPath("$[0].nome").value("Orçamento por CC"))
            .andExpect(jsonPath("$[0].tipo").value("PACOTE"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarCatalogo_retornaItensVisiveisNaHierarquia() throws Exception {
        when(templatePublishService.listarCatalogo("user-a")).thenReturn(List.of(
            new TemplateCatalogItemDTO(1L, "Vendas", TemplateTipo.DATASET, 2, 2, false, 10L, null, null)));

        mockMvc.perform(get("/workspace/templates/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Vendas"))
            .andExpect(jsonPath("$[0].versaoAtual").value(2));
    }

    @Test
    @WithMockUser(username = "user-b", roles = "USER")
    void listarCatalogo_usuarioForaHierarquia_catalogoVazio() throws Exception {
        when(templatePublishService.listarCatalogo("user-b")).thenReturn(List.of());

        mockMvc.perform(get("/workspace/templates/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void listarCatalogo_comAtualizacaoDisponivel_indicaFlag() throws Exception {
        when(templatePublishService.listarCatalogo("user-a")).thenReturn(List.of(
            new TemplateCatalogItemDTO(2L, "KPI", TemplateTipo.WIDGET, 3, 3, true, 11L, 5L, 1)));

        mockMvc.perform(get("/workspace/templates/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].atualizacaoDisponivel").value(true));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_nativoOrcamento_idZero_retorna201() throws Exception {
        when(templateInstallService.instalar(
                eq("user-a"), eq(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID), eq(1L)))
            .thenReturn(new TemplateInstallResultDTO(null, 0L, 1, 1L, 50L, List.of(60L, 61L), Map.of()));

        mockMvc.perform(post("/workspace/templates/0/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.templateId").value(0))
            .andExpect(jsonPath("$.datasetId").value(50));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_valido_retorna201() throws Exception {
        when(templateInstallService.instalar(eq("user-a"), eq(3L), eq(1L)))
            .thenReturn(new TemplateInstallResultDTO(10L, 3L, 1, 1L, 50L, List.of(), Map.of("primary", 50L)));

        mockMvc.perform(post("/workspace/templates/3/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.datasetId").value(50));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_templateInexistente_retorna404() throws Exception {
        when(templateInstallService.instalar(eq("user-a"), eq(99L), eq(1L)))
            .thenThrow(new WorkspaceTemplateNotFoundException(99L));

        mockMvc.perform(post("/workspace/templates/99/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_foraHierarquia_retorna403() throws Exception {
        when(templateInstallService.instalar(eq("user-a"), eq(4L), eq(1L)))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(post("/workspace/templates/4/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":1}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_workspaceInvalido_retorna404() throws Exception {
        when(templateInstallService.instalar(eq("user-a"), eq(3L), eq(99L)))
            .thenThrow(new WorkspaceNotFoundException(99L));

        mockMvc.perform(post("/workspace/templates/3/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workspaceId\":99}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void upgradeVersao_valido_retorna200() throws Exception {
        when(templateInstallService.atualizarVersao("user-a", 7L))
            .thenReturn(new TemplateInstallResultDTO(7L, 3L, 2, 1L, 50L, List.of(), Map.of()));

        mockMvc.perform(post("/workspace/templates/installations/7/upgrade"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versaoInstalada").value(2));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void upgradeVersao_instalacaoInexistente_retorna404() throws Exception {
        when(templateInstallService.atualizarVersao("user-a", 999L))
            .thenThrow(new WorkspaceTemplateInstallationNotFoundException(999L));

        mockMvc.perform(post("/workspace/templates/installations/999/upgrade"))
            .andExpect(status().isNotFound());
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
            .andExpect(jsonPath("$.datasetId").value(10));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void publicar_ambosIds_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/templates/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"datasetId\":1,\"widgetDefinitionId\":2}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listarCatalogo_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/templates/catalog"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void instalarTemplate_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/templates/3/install")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
