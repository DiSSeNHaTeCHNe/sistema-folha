package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.RubricaDTO;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.repository.RubricaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rubricas")
@RequiredArgsConstructor
@Tag(name = "Rubricas", description = "API para gerenciamento de rubricas")
public class RubricaController {
    private final RubricaRepository rubricaRepository;

    @GetMapping
    @Operation(summary = "Lista todas as rubricas ativas")
    public ResponseEntity<List<RubricaDTO>> listar() {
        List<RubricaDTO> rubricas = rubricaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(rubricas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma rubrica ativa pelo ID")
    public ResponseEntity<RubricaDTO> buscarPorId(@PathVariable Long id) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(rubrica -> ResponseEntity.ok(toDTO(rubrica)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova rubrica")
    public ResponseEntity<RubricaDTO> cadastrar(@Valid @RequestBody RubricaDTO dto) {
        if (rubricaRepository.existsByCodigo(dto.codigo())) {
            return ResponseEntity.badRequest().build();
        }

        Rubrica rubrica = toEntity(dto);
        rubrica = rubricaRepository.save(rubrica);
        return ResponseEntity.ok(toDTO(rubrica));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma rubrica existente")
    public ResponseEntity<RubricaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody RubricaDTO dto) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(rubrica -> {
                Rubrica rubricaAtualizada = toEntity(dto);
                rubricaAtualizada.setId(id);
                rubricaAtualizada = rubricaRepository.save(rubricaAtualizada);
                return ResponseEntity.ok(toDTO(rubricaAtualizada));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma rubrica (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(rubrica -> {
                rubricaRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private RubricaDTO toDTO(Rubrica rubrica) {
        return new RubricaDTO(
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getDescricao(),
            rubrica.getTipo(),
            rubrica.getPorcentagem()
        );
    }

    private Rubrica toEntity(RubricaDTO dto) {
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(dto.codigo());
        rubrica.setDescricao(dto.descricao());
        rubrica.setTipo(dto.tipo());
        rubrica.setPorcentagem(dto.porcentagem());
        rubrica.setAtivo(true);
        return rubrica;
    }
} 