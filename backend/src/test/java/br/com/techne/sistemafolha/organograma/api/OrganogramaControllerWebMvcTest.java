package br.com.techne.sistemafolha.organograma.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.organograma.application.OrganogramaService;
import br.com.techne.sistemafolha.organograma.domain.NoOrganogramaNotFoundException;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganogramaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrganogramaControllerWebMvcTest {

    private static final String BODY_ATUALIZAR = """
        {
          "id": 1,
          "nome": "Raiz",
          "nivel": 0,
          "posicao": 0,
          "ativo": true,
          "organogramaAtivo": false
        }
        """;

    private static NoOrganogramaDTO noExemplo() {
        return new NoOrganogramaDTO(
            1L, "Raiz", null, 0, null, null, 0, true, false,
            List.of(), null, List.of(), null, List.of(), null, null, null, null);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganogramaService organogramaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTodos_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/organograma"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarTodos_retorna200() throws Exception {
        when(organogramaService.listarTodos()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/organograma")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(organogramaService.buscarPorId(1L)).thenReturn(noExemplo());
        mockMvc.perform(get("/organograma/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(organogramaService.buscarPorId(99L)).thenThrow(new NoOrganogramaNotFoundException(99L));
        mockMvc.perform(get("/organograma/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_sucesso_retorna200() throws Exception {
        when(organogramaService.cadastrar(any())).thenReturn(noExemplo());
        mockMvc.perform(post("/organograma")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Raiz\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_regraInvalida_retorna400() throws Exception {
        when(organogramaService.cadastrar(any())).thenThrow(new IllegalArgumentException("Inválido"));
        mockMvc.perform(post("/organograma")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Raiz\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_erroInesperado_retorna500() throws Exception {
        when(organogramaService.cadastrar(any())).thenThrow(new RuntimeException("boom"));
        mockMvc.perform(post("/organograma")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Raiz\"}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(organogramaService.atualizar(eq(1L), any())).thenReturn(noExemplo());
        mockMvc.perform(put("/organograma/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_ATUALIZAR))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_naoEncontrado_retorna404() throws Exception {
        when(organogramaService.atualizar(eq(99L), any())).thenThrow(new NoOrganogramaNotFoundException(99L));
        mockMvc.perform(put("/organograma/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_ATUALIZAR.replace("\"id\": 1", "\"id\": 99")))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/organograma/1")).andExpect(status().isNoContent());
        verify(organogramaService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_comFilhos_retorna400() throws Exception {
        doThrow(new IllegalStateException("Tem filhos")).when(organogramaService).remover(1L);
        mockMvc.perform(delete("/organograma/1")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void removerComFilhos_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/organograma/1/cascata")).andExpect(status().isNoContent());
        verify(organogramaService).removerComFilhos(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void obterArvoreCompleta_retorna200() throws Exception {
        when(organogramaService.obterArvoreCompleta()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/organograma/arvore")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void obterFilhos_retorna200() throws Exception {
        when(organogramaService.obterFilhos(isNull())).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/organograma/filhos")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void moverNo_sucesso_retorna200() throws Exception {
        when(organogramaService.moverNo(eq(1L), isNull(), eq(0))).thenReturn(noExemplo());
        mockMvc.perform(put("/organograma/1/mover").param("novaPosicao", "0"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void moverNo_naoEncontrado_retorna404() throws Exception {
        when(organogramaService.moverNo(eq(99L), isNull(), isNull()))
            .thenThrow(new NoOrganogramaNotFoundException(99L));
        mockMvc.perform(put("/organograma/99/mover")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void obterOrganogramaAtivo_encontrado_retorna200() throws Exception {
        when(organogramaService.obterOrganogramaAtivo()).thenReturn(noExemplo());
        mockMvc.perform(get("/organograma/ativo")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void obterOrganogramaAtivo_ausente_retorna404() throws Exception {
        when(organogramaService.obterOrganogramaAtivo()).thenReturn(null);
        mockMvc.perform(get("/organograma/ativo")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void ativarOrganograma_sucesso_retorna200() throws Exception {
        mockMvc.perform(put("/organograma/1/ativar")).andExpect(status().isOk());
        verify(organogramaService).ativarOrganograma(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void desativarOrganograma_retorna200() throws Exception {
        mockMvc.perform(put("/organograma/desativar")).andExpect(status().isOk());
        verify(organogramaService).desativarOrganograma();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void associarFuncionario_sucesso_retorna200() throws Exception {
        when(organogramaService.associarFuncionario(1L, 10L))
            .thenReturn(new FuncionarioOrganogramaDTO(1L, 10L, null, 1L, null, null, null));
        mockMvc.perform(post("/organograma/1/funcionarios/10")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void associarFuncionario_naoEncontrado_retorna404() throws Exception {
        when(organogramaService.associarFuncionario(1L, 99L))
            .thenThrow(new FuncionarioNotFoundException(99L));
        mockMvc.perform(post("/organograma/1/funcionarios/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void desassociarFuncionario_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/organograma/1/funcionarios/10")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarFuncionariosPorNo_retorna200() throws Exception {
        when(organogramaService.listarFuncionariosPorNo(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/organograma/1/funcionarios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void associarCentroCusto_sucesso_retorna200() throws Exception {
        when(organogramaService.associarCentroCusto(1L, 2L))
            .thenReturn(new CentroCustoOrganogramaDTO(1L, 2L, null, 1L, null, null, null));
        mockMvc.perform(post("/organograma/1/centros-custo/2")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void associarCentroCusto_naoEncontrado_retorna404() throws Exception {
        when(organogramaService.associarCentroCusto(1L, 99L))
            .thenThrow(new CentroCustoNotFoundException(99L));
        mockMvc.perform(post("/organograma/1/centros-custo/99")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void desassociarCentroCusto_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/organograma/1/centros-custo/2")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarCentrosCustoPorNo_retorna200() throws Exception {
        when(organogramaService.listarCentrosCustoPorNo(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/organograma/1/centros-custo")).andExpect(status().isOk());
    }
}
