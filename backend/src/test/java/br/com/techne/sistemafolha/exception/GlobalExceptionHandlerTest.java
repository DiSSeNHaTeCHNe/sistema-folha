package br.com.techne.sistemafolha.exception;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
}
