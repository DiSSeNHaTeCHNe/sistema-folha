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
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowValidationException;
import br.com.techne.sistemafolha.workspace.domain.InvalidFormulaException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNameConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinitionNotFoundException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioAcessoNegadoException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioGeracaoLimiteException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioIndisponivelException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

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

    @ExceptionHandler(RelatorioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRelatorioNotFoundException(RelatorioNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RelatorioIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleRelatorioIndisponivelException(RelatorioIndisponivelException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RelatorioGeracaoLimiteException.class)
    public ResponseEntity<ErrorResponse> handleRelatorioGeracaoLimiteException(RelatorioGeracaoLimiteException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }


    @ExceptionHandler(DashboardAcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> handleDashboardAcessoNegadoException(DashboardAcessoNegadoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(WorkspaceAcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceAcessoNegadoException(WorkspaceAcessoNegadoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(WorkspaceDatasetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceDatasetNotFoundException(WorkspaceDatasetNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkspaceDatasetRowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceDatasetRowNotFoundException(
            WorkspaceDatasetRowNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkspaceWidgetDefinitionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceWidgetDefinitionNotFoundException(
            WorkspaceWidgetDefinitionNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceNotFoundException(WorkspaceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkspaceNameConflictException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceNameConflictException(WorkspaceNameConflictException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(WorkspaceDatasetConflictException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceDatasetConflictException(
            WorkspaceDatasetConflictException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(WorkspaceQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceQuotaExceededException(WorkspaceQuotaExceededException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
            WorkspaceQuotaExceededException.CODE + ": " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DatasetRowValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleDatasetRowValidationException(
            DatasetRowValidationException ex) {
        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            ex.getErrors().stream()
                .map(fieldError -> new FieldErrorItem(fieldError.field(), fieldError.message()))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidFormulaException.class)
    public ResponseEntity<ValidationErrorResponse> handleInvalidFormulaException(InvalidFormulaException ex) {
        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            ex.getErrors().stream()
                .map(formulaError -> new FieldErrorItem("formula", formulaError))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RelatorioAcessoNegadoException.class)
    public ResponseEntity<ErrorResponse> handleRelatorioAcessoNegadoException(RelatorioAcessoNegadoException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getParameterName() + ": obrigatório");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Acesso negado");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
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