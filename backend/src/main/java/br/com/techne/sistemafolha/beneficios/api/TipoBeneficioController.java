package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.beneficios.application.TipoBeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tipo-beneficio")
@RequiredArgsConstructor
@Tag(name = "Tipos de Benefício", description = "API para gerenciamento de tipos de benefício")
public class TipoBeneficioController {

    private final TipoBeneficioService tipoBeneficioService;

    @GetMapping
    @Operation(summary = "Lista todos os tipos de benefício ativos")
    public ResponseEntity<List<TipoBeneficioDTO>> listarAtivos() {
        return ResponseEntity.ok(tipoBeneficioService.listarAtivos());
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo tipo de benefício")
    public ResponseEntity<TipoBeneficioDTO> criar(
            @Parameter(description = "Dados do tipo de benefício") @Valid @RequestBody TipoBeneficioDTO dto) {
        return ResponseEntity.ok(tipoBeneficioService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza a descrição de um tipo de benefício")
    public ResponseEntity<TipoBeneficioDTO> atualizar(
            @Parameter(description = "ID do tipo de benefício") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do tipo de benefício") @Valid @RequestBody TipoBeneficioDTO dto) {
        return ResponseEntity.ok(tipoBeneficioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um tipo de benefício (soft delete)")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do tipo de benefício") @PathVariable Long id) {
        tipoBeneficioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
