package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.FormulaValidationService;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FormulaValidationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FormulaValidationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FormulaValidationService formulaValidationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void validar_semAuth_retorna403() throws Exception {
        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"formula":"SOMA(valor)","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void validar_semEscopo_retorna403() throws Exception {
        when(formulaValidationService.validar(eq("user-a"), any(FormulaValidationRequest.class)))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"formula":"SOMA(valor)","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void validar_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formula\":\"\",\"fontes\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void validar_formulaInvalida_retorna400ComErros() throws Exception {
        when(formulaValidationService.validar(eq("user-a"), any(FormulaValidationRequest.class)))
            .thenReturn(new FormulaValidationResponseDTO(false, List.of("Campo inválido: x")));

        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"formula":"SOMA(x)","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors[0]").value("Campo inválido: x"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void validar_formulaValida_retorna200() throws Exception {
        when(formulaValidationService.validar(eq("user-a"), any(FormulaValidationRequest.class)))
            .thenReturn(new FormulaValidationResponseDTO(true, List.of()));

        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"formula":"SOMA(total_proventos)","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @WithMockUser(username = "user-b", roles = "USER")
    void validar_isolamentoUsuarios_usaLoginCorreto() throws Exception {
        when(formulaValidationService.validar(eq("user-b"), any(FormulaValidationRequest.class)))
            .thenReturn(new FormulaValidationResponseDTO(true, List.of()));

        mockMvc.perform(post("/workspace/formulas/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"formula":"SOMA(total_proventos)","fontes":[{"kind":"SISTEMA","ref":"FOLHA"}]}"""))
            .andExpect(status().isOk());

        verify(formulaValidationService).validar(eq("user-b"), any(FormulaValidationRequest.class));
    }
}
