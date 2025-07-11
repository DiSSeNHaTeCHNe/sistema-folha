package br.com.techne.sistemafolha.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);
    
    private final RefreshTokenService refreshTokenService;

    // Executa a limpeza de tokens expirados a cada 6 horas
    @Scheduled(fixedRate = 21600000) // 6 horas em milissegundos
    public void limparTokensExpirados() {
        logger.info("Iniciando limpeza de refresh tokens expirados");
        
        try {
            refreshTokenService.limparTokensExpirados();
            logger.info("Limpeza de refresh tokens expirados concluída com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao limpar refresh tokens expirados: {}", e.getMessage(), e);
        }
    }
} 