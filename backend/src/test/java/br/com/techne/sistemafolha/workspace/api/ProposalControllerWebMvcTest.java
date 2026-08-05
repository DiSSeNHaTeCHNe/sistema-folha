package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.ApiKeySecurity;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.WorkspaceProposalService;
import br.com.techne.sistemafolha.workspace.domain.ProposalPayload;
import br.com.techne.sistemafolha.workspace.domain.ProposalStatus;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceIaPermissaoNegadaException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceProposalExpiredException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceProposalNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProposalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProposalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceProposalService proposalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void criar_semAuth_retorna403() throws Exception {
        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"teste\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_semPermissaoIa_retorna403SemCorpoProposta() throws Exception {
        when(proposalService.criarProposta(eq("user-a"), any()))
            .thenThrow(new WorkspaceIaPermissaoNegadaException());

        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"crie dataset\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(WorkspaceIaPermissaoNegadaException.MESSAGE))
            .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void criar_comPermissao_retorna201() throws Exception {
        when(proposalService.criarProposta(eq("ia-user"), any())).thenReturn(sampleProposal(1L));

        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"previsão com competencia\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void obter_retorna200() throws Exception {
        when(proposalService.obter("ia-user", 5L)).thenReturn(sampleProposal(5L));

        mockMvc.perform(get("/workspace/proposals/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void confirmar_formulaOversized_retorna400() throws Exception {
        String oversizedFormula = "A".repeat(2001);

        mockMvc.perform(post("/workspace/proposals/2/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formula\":\"" + oversizedFormula + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("formula")));
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void confirmar_nomeOversized_retorna400() throws Exception {
        String oversizedNome = "A".repeat(121);

        mockMvc.perform(post("/workspace/proposals/2/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + oversizedNome + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nome")));
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void confirmar_retorna200() throws Exception {
        ProposalDTO aplicada = new ProposalDTO(
            2L, ProposalStatus.APLICADA, new ProposalPayload(), 10L,
            LocalDateTime.now(), LocalDateTime.now().plusHours(72), LocalDateTime.now());
        when(proposalService.confirmar(eq("ia-user"), eq(2L), isNull())).thenReturn(aplicada);

        mockMvc.perform(post("/workspace/proposals/2/confirmar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APLICADA"));
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void descartar_retorna204() throws Exception {
        mockMvc.perform(post("/workspace/proposals/3/descartar"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void obter_naoEncontrada_retorna404() throws Exception {
        when(proposalService.obter("ia-user", 99L)).thenThrow(new WorkspaceProposalNotFoundException(99L));

        mockMvc.perform(get("/workspace/proposals/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void confirmar_expirada_retorna410() throws Exception {
        when(proposalService.confirmar(eq("ia-user"), eq(4L), isNull()))
            .thenThrow(new WorkspaceProposalExpiredException());

        mockMvc.perform(post("/workspace/proposals/4/confirmar"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.message").value(WorkspaceProposalExpiredException.MESSAGE));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void criar_semEscopo_retorna403() throws Exception {
        when(proposalService.criarProposta(eq("user-a"), any()))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"x\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void criar_comApiKeyWorkspaceMarker_permitePost() throws Exception {
        configurarApiKeyWorkspace("agent-key");
        when(proposalService.criarProposta(eq("agent-key"), any())).thenReturn(sampleProposal(7L));

        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"via mcp\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void confirmar_comApiKeyWorkspaceMarker_permitePost() throws Exception {
        configurarApiKeyWorkspace("agent-key");
        when(proposalService.confirmar(eq("agent-key"), eq(8L), isNull()))
            .thenReturn(sampleProposal(8L));

        mockMvc.perform(post("/workspace/proposals/8/confirmar"))
            .andExpect(status().isOk());
    }

    @Test
    void criar_comApiKeyReadOnly_bloqueadoPeloWriteGuard() throws Exception {
        configurarApiKeyReadOnly("readonly-key");

        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"DATASET\",\"descricaoNatural\":\"x\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void criar_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"\",\"descricaoNatural\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "ia-user", roles = "USER")
    void descartar_servicoPropagaErro404() throws Exception {
        doThrow(new WorkspaceProposalNotFoundException(55L))
            .when(proposalService).descartar("ia-user", 55L);

        mockMvc.perform(post("/workspace/proposals/55/descartar"))
            .andExpect(status().isNotFound());
    }

    private ProposalDTO sampleProposal(Long id) {
        ProposalPayload payload = new ProposalPayload();
        payload.setKind("DATASET");
        payload.setNome("Sugerido");
        return new ProposalDTO(
            id,
            ProposalStatus.PENDENTE,
            payload,
            10L,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(72),
            null
        );
    }

    private void configurarApiKeyWorkspace(String login) {
        var user = User.builder()
            .username(login)
            .password("n/a")
            .authorities(List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority(ApiKeySecurity.ROLE_API_KEY_WORKSPACE)))
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void configurarApiKeyReadOnly(String login) {
        var user = User.builder()
            .username(login)
            .password("n/a")
            .authorities(List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority(ApiKeySecurity.ROLE_API_KEY_READONLY)))
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
