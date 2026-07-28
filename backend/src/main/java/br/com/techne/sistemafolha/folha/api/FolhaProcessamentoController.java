package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.folha.application.FolhaProcessamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "API para consulta e processamento de folha de pagamento")
public class FolhaProcessamentoController {

    private final FolhaProcessamentoService folhaProcessamentoService;

    @PostMapping("/processar")
    @Operation(summary = "Processa competência: copia ADP para ficha e recalcula totalizadores")
    public ResponseEntity<ProcessamentoResultadoDTO> processar(@Valid @RequestBody ProcessamentoRequestDTO request) {
        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            request.competenciaInicio(),
            request.competenciaFim(),
            request.decimoTerceiro(),
            request.opcoes()
        );
        return ResponseEntity.ok(resultado);
    }
}
