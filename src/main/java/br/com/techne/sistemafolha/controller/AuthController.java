package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.LoginDTO;
import br.com.techne.sistemafolha.dto.RefreshTokenRequest;
import br.com.techne.sistemafolha.dto.TokenDTO;
import br.com.techne.sistemafolha.security.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "APIs de autenticação")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário", description = "Retorna um token JWT e um refresh token para autenticação")
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid LoginDTO loginDTO) {
        return ResponseEntity.ok(authenticationService.authenticate(loginDTO));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token de acesso", description = "Gera um novo token JWT usando o refresh token")
    public ResponseEntity<TokenDTO> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Realiza logout do usuário", description = "Revoga o refresh token do usuário")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }
} 