package br.com.techne.sistemafolha.exception;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalDuplicadaException;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.ImportacaoBeneficioMensalInvalidaException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioCodigoDuplicadoException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.auth.domain.ApiKeyNotFoundException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FuncionarioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFuncionarioNotFoundException(FuncionarioNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FuncionarioJaExisteException.class)
    public ResponseEntity<ErrorResponse> handleFuncionarioJaExisteException(FuncionarioJaExisteException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RubricaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRubricaNotFoundException(RubricaNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FichaMensalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFichaMensalNotFoundException(FichaMensalNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CentroCustoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCentroCustoNotFoundException(CentroCustoNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoOrganogramaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoOrganogramaNotFoundException(NoOrganogramaNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrganogramaAtivoConflictException.class)
    public ResponseEntity<ErrorResponse> handleOrganogramaAtivoConflictException(OrganogramaAtivoConflictException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TipoBeneficioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTipoBeneficioNotFoundException(TipoBeneficioNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TipoBeneficioCodigoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleTipoBeneficioCodigoDuplicadoException(TipoBeneficioCodigoDuplicadoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FuncionarioRubricaFixaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFuncionarioRubricaFixaNotFoundException(
            FuncionarioRubricaFixaNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FuncionarioRubricaFixaVigenciaConflictException.class)
    public ResponseEntity<ErrorResponse> handleFuncionarioRubricaFixaVigenciaConflictException(
            FuncionarioRubricaFixaVigenciaConflictException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BeneficioMensalNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBeneficioMensalNotFoundException(BeneficioMensalNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BeneficioMensalDuplicadaException.class)
    public ResponseEntity<ErrorResponse> handleBeneficioMensalDuplicadaException(BeneficioMensalDuplicadaException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ImportacaoBeneficioMensalInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleImportacaoBeneficioMensalInvalidaException(
            ImportacaoBeneficioMensalInvalidaException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenInvalidoException(RefreshTokenInvalidoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyNotFoundException(ApiKeyNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining("; "));
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Ocorreu um erro interno no servidor"
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 