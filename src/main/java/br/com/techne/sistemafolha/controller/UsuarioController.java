package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.UsuarioDTO;
import br.com.techne.sistemafolha.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "API para gerenciamento de usuários")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista todos os usuários ativos")
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo ID")
    public ResponseEntity<UsuarioDTO> buscarPorId(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/login/{login}")
    @Operation(summary = "Busca um usuário pelo login")
    public ResponseEntity<UsuarioDTO> buscarPorLogin(
            @Parameter(description = "Login do usuário") @PathVariable String login) {
        return ResponseEntity.ok(usuarioService.buscarPorLogin(login));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo usuário")
    public ResponseEntity<UsuarioDTO> cadastrar(
            @Parameter(description = "Dados do usuário") @Valid @RequestBody UsuarioDTO usuario) {
        return ResponseEntity.ok(usuarioService.cadastrar(usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um usuário existente")
    public ResponseEntity<UsuarioDTO> atualizar(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do usuário") @Valid @RequestBody UsuarioDTO usuario) {
        return ResponseEntity.ok(usuarioService.atualizar(id, usuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um usuário")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        usuarioService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/alterar-senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha) {
        try {
            usuarioService.alterarSenha(id, senhaAtual, novaSenha);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 