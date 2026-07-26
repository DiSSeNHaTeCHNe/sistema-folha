package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.BeneficioMensalDTO;
import br.com.techne.sistemafolha.dto.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.exception.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.model.BeneficioMensal;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.repository.BeneficioMensalRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import br.com.techne.sistemafolha.service.BeneficioMensalService;
import br.com.techne.sistemafolha.service.OrganogramaAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/beneficio-mensal")
@RequiredArgsConstructor
@Tag(name = "Benefícios Mensais", description = "API para consulta e lançamento de benefícios mensais")
public class BeneficioMensalController {

    private final BeneficioMensalService beneficioMensalService;
    private final BeneficioMensalRepository beneficioMensalRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganogramaAcessoService organogramaAcessoService;

    @GetMapping
    @Operation(summary = "Lista lançamentos de benefícios mensais por competência")
    public ResponseEntity<List<BeneficioMensalDTO>> listarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        return ResponseEntity.ok(
                beneficioMensalService.listarPorCompetencia(competenciaInicio, competenciaFim, centrosAcessiveis));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo de benefícios mensais agrupado por tipo na competência")
    public ResponseEntity<List<BeneficioMensalResumoDTO>> resumoPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        return ResponseEntity.ok(
                beneficioMensalService.resumoPorCompetencia(competenciaInicio, competenciaFim, centrosAcessiveis));
    }

    @GetMapping("/funcionario/{id}")
    @Operation(summary = "Lista lançamentos de benefícios mensais de um funcionário na competência")
    public ResponseEntity<List<BeneficioMensalDTO>> listarPorFuncionario(
            @Parameter(description = "ID do funcionário") @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        List<BeneficioMensalDTO> beneficios = beneficioMensalService
                .listarPorFuncionario(id, competenciaInicio, competenciaFim)
                .stream()
                .filter(dto -> aplicarFiltroAcesso(dto, centrosAcessiveis))
                .collect(Collectors.toList());
        return ResponseEntity.ok(beneficios);
    }

    @PostMapping
    @Operation(summary = "Cria um lançamento manual de benefício mensal")
    public ResponseEntity<BeneficioMensalDTO> criar(
            @Parameter(description = "Dados do lançamento") @Valid @RequestBody BeneficioMensalDTO dto,
            Authentication authentication) {
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
        if (!aplicarFiltroAcesso(funcionario, centrosAcessiveis)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(beneficioMensalService.criar(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um lançamento de benefício mensal (soft delete)")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do lançamento") @PathVariable Long id,
            Authentication authentication) {
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        return beneficioMensalRepository.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .filter(b -> aplicarFiltroAcesso(b, centrosAcessiveis))
                .map(b -> {
                    beneficioMensalService.remover(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Set<Long> obterCentrosAcessiveis(Authentication authentication) {
        String login = authentication.getName();
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return organogramaAcessoService.obterCentrosCustoAcessiveis(usuario.getId());
    }

    private boolean aplicarFiltroAcesso(BeneficioMensal beneficio, Set<Long> centrosAcessiveis) {
        return aplicarFiltroAcesso(beneficio.getFuncionario(), centrosAcessiveis);
    }

    private boolean aplicarFiltroAcesso(Funcionario funcionario, Set<Long> centrosAcessiveis) {
        if (centrosAcessiveis.isEmpty()) {
            return true;
        }
        if (funcionario != null && funcionario.getCentroCusto() != null) {
            return centrosAcessiveis.contains(funcionario.getCentroCusto().getId());
        }
        return false;
    }

    private boolean aplicarFiltroAcesso(BeneficioMensalDTO dto, Set<Long> centrosAcessiveis) {
        if (centrosAcessiveis.isEmpty()) {
            return true;
        }
        if (dto.centroCustoId() != null) {
            return centrosAcessiveis.contains(dto.centroCustoId());
        }
        return false;
    }
}
