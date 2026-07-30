package br.com.techne.sistemafolha.auth.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Test
    void limparTokensExpirados_delegatesToRefreshTokenService() {
        tokenCleanupService.limparTokensExpirados();

        verify(refreshTokenService).limparTokensExpirados();
    }

    @Test
    void limparTokensExpirados_quandoFalha_naoPropagaExcecao() {
        doThrow(new RuntimeException("falha no banco")).when(refreshTokenService).limparTokensExpirados();

        tokenCleanupService.limparTokensExpirados();

        verify(refreshTokenService).limparTokensExpirados();
    }
}
