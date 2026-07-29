package br.com.techne.sistemafolha.organograma.api;

import br.com.techne.sistemafolha.config.SecurityConfig;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganogramaController.class)
@Import(SecurityConfig.class)
class OrganogramaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganogramaService organogramaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarTodos_retorna200() throws Exception {
        when(organogramaService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/organograma"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(organogramaService.buscarPorId(99L)).thenThrow(new NoOrganogramaNotFoundException(99L));

        mockMvc.perform(get("/organograma/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_jsonValido_retorna200() throws Exception {
        NoOrganogramaDTO dto = new NoOrganogramaDTO(
            1L, "Raiz", null, 0, null, null, 0, true, false,
            List.of(), null, List.of(), null, List.of(), null, null, null, null
        );
        when(organogramaService.cadastrar(org.mockito.ArgumentMatchers.any())).thenReturn(dto);

        mockMvc.perform(post("/organograma")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Raiz\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void obterArvoreCompleta_retorna200() throws Exception {
        when(organogramaService.obterArvoreCompleta()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/organograma/arvore"))
            .andExpect(status().isOk());
    }
}
