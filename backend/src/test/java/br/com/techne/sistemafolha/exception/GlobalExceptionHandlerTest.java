package br.com.techne.sistemafolha.exception;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalDuplicadaException;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.ImportacaoBeneficioMensalInvalidaException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioCodigoDuplicadoException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.auth.domain.RefreshTokenInvalidoException;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioJaExisteException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaVigenciaConflictException;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import br.com.techne.sistemafolha.folha.domain.FichaMensalNotFoundException;
import br.com.techne.sistemafolha.organograma.domain.NoOrganogramaNotFoundException;
import br.com.techne.sistemafolha.organograma.domain.OrganogramaAtivoConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleFuncionarioNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleFuncionarioNotFoundException(
            new FuncionarioNotFoundException(42L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Funcionário não encontrado com ID: 42", response.getBody().message());
    }

    @Test
    void handleIllegalArgumentException_retorna400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(
            new IllegalArgumentException("Parâmetro inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Parâmetro inválido", response.getBody().message());
    }

    @Test
    void handleBeneficioMensalNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleBeneficioMensalNotFoundException(
            new BeneficioMensalNotFoundException(99L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Benefício mensal não encontrado com ID: 99", response.getBody().message());
    }

    @Test
    void handleMethodArgumentNotValidException_retorna400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "login", "must not be blank"));
        bindingResult.addError(new FieldError("request", "senha", "size must be between 8 and 64"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(
            "login: must not be blank; senha: size must be between 8 and 64",
            response.getBody().message()
        );
    }

    @Test
    void handleFuncionarioJaExisteException_retorna400() {
        ResponseEntity<ErrorResponse> response = handler.handleFuncionarioJaExisteException(
            new FuncionarioJaExisteException("CPF duplicado"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("CPF duplicado", response.getBody().message());
    }

    @Test
    void handleRubricaNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleRubricaNotFoundException(
            new RubricaNotFoundException("Rubrica não encontrada"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Rubrica não encontrada", response.getBody().message());
    }

    @Test
    void handleFichaMensalNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleFichaMensalNotFoundException(
            new FichaMensalNotFoundException(7L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ficha mensal não encontrada: 7", response.getBody().message());
    }

    @Test
    void handleCentroCustoNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleCentroCustoNotFoundException(
            new CentroCustoNotFoundException(3L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Centro de custo não encontrado com ID: 3", response.getBody().message());
    }

    @Test
    void handleNoOrganogramaNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleNoOrganogramaNotFoundException(
            new NoOrganogramaNotFoundException(11L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Nó do organograma não encontrado com ID: 11", response.getBody().message());
    }

    @Test
    void handleOrganogramaAtivoConflictException_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleOrganogramaAtivoConflictException(
            new OrganogramaAtivoConflictException());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Já existe um organograma ativo. Apenas um organograma pode estar ativo por vez.",
            response.getBody().message());
    }

    @Test
    void handleTipoBeneficioNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleTipoBeneficioNotFoundException(
            new TipoBeneficioNotFoundException(5L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Tipo de benefício não encontrado com ID: 5", response.getBody().message());
    }

    @Test
    void handleTipoBeneficioCodigoDuplicadoException_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleTipoBeneficioCodigoDuplicadoException(
            new TipoBeneficioCodigoDuplicadoException("VR"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Já existe um tipo de benefício com o código: VR", response.getBody().message());
    }

    @Test
    void handleFuncionarioRubricaFixaNotFoundException_retorna404() {
        ResponseEntity<ErrorResponse> response = handler.handleFuncionarioRubricaFixaNotFoundException(
            new FuncionarioRubricaFixaNotFoundException(8L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Rubrica fixa não encontrada: 8", response.getBody().message());
    }

    @Test
    void handleFuncionarioRubricaFixaVigenciaConflictException_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleFuncionarioRubricaFixaVigenciaConflictException(
            FuncionarioRubricaFixaVigenciaConflictException.forIndividual());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody().message());
    }

    @Test
    void handleBeneficioMensalDuplicadaException_retorna409() {
        ResponseEntity<ErrorResponse> response = handler.handleBeneficioMensalDuplicadaException(
            new BeneficioMensalDuplicadaException("Benefício duplicado", "2024-01-01", "2024-01-31"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody().message());
    }

    @Test
    void handleImportacaoBeneficioMensalInvalidaException_retorna400() {
        ResponseEntity<ErrorResponse> response = handler.handleImportacaoBeneficioMensalInvalidaException(
            new ImportacaoBeneficioMensalInvalidaException(java.util.List.of("linha inválida")));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().message());
    }

    @Test
    void handleRefreshTokenInvalidoException_retorna401() {
        ResponseEntity<ErrorResponse> response = handler.handleRefreshTokenInvalidoException(
            new RefreshTokenInvalidoException("Refresh token inválido ou expirado"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals("Refresh token inválido ou expirado", response.getBody().message());
    }

    @Test
    void handleAccessDeniedException_retorna403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(
            new AccessDeniedException("Permissão API_KEY necessária"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("Acesso negado", response.getBody().message());
    }

    @Test
    void handleGenericException_retorna500() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocorreu um erro interno no servidor", response.getBody().message());
    }
}
