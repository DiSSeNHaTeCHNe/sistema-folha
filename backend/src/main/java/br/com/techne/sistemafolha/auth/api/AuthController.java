package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "APIs de autenticação")
public class AuthController {

    private final AuthenticationService authenticationService;

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

    @GetMapping("/acesso")
    @Operation(summary = "Obtém informações de acesso do usuário",
               description = "Retorna os centros de custo e nó do organograma que o usuário pode acessar")
    public ResponseEntity<AcessoUsuarioDTO> obterInformacoesAcesso(Authentication authentication) {
        return ResponseEntity.ok(authenticationService.obterAcessoUsuarioPorLogin(authentication.getName()));
    }
}
