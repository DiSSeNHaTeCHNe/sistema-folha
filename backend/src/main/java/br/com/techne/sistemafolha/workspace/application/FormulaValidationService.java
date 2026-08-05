package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.FormulaValidationRequest;
import br.com.techne.sistemafolha.workspace.api.FormulaValidationResponseDTO;
import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormulaValidationService {

    private final WidgetDefinitionService widgetDefinitionService;
    private final FormulaEngine formulaEngine;

    @Transactional(readOnly = true)
    public FormulaValidationResponseDTO validar(String login, FormulaValidationRequest request) {
        List<AvailableField> fields = widgetDefinitionService.buildAvailableFields(login, request.fontes());
        FormulaValidationResult result = formulaEngine.validate(request.formula(), fields);
        return new FormulaValidationResponseDTO(result.valid(), result.errors());
    }
}
