package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.ApiKeyCreateRequest;
import br.com.techne.sistemafolha.auth.api.ApiKeyCreatedDTO;
import br.com.techne.sistemafolha.auth.api.ApiKeyListDTO;
import br.com.techne.sistemafolha.auth.domain.ApiKey;
import br.com.techne.sistemafolha.auth.domain.ApiKeyNotFoundException;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.ApiKeyRepository;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    static final String CHAVE_PREFIX = "sf_live_";
    static final String PERMISSAO_API_KEY = "API_KEY";
    static final String PERMISSAO_ADMIN = "ADMIN";
    static final int DEFAULT_DIAS_VALIDADE = 365;
    static final int SECRET_BYTES = 32;
    static final int PREFIXO_RANDOM_CHARS = 8;

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String DOMAIN = "auth";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PREFIXO_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final ApiKeyRepository apiKeyRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public ApiKeyCreatedDTO criar(Usuario usuario, ApiKeyCreateRequest request) {
        exigirPermissaoApiKey(usuario);

        int diasValidade = request.diasValidade() != null ? request.diasValidade() : DEFAULT_DIAS_VALIDADE;
        if (diasValidade < 1 || diasValidade > 365) {
            throw new IllegalArgumentException("diasValidade deve estar entre 1 e 365");
        }

        String prefixo = gerarPrefixo();
        String secret = gerarSecret();
        String chave = prefixo + secret;
        String hashChave = passwordEncoder.encode(chave);

        ApiKey apiKey = new ApiKey();
        apiKey.setUsuario(usuario);
        apiKey.setNome(request.nome().trim());
        apiKey.setPrefixo(prefixo);
        apiKey.setHashChave(hashChave);
        apiKey.setEscopo(ApiKey.ESCOPO_READ);
        apiKey.setDataExpiracao(LocalDateTime.now(clock).plusDays(diasValidade));
        apiKey.setRevogado(false);

        ApiKey salva = apiKeyRepository.save(apiKey);
        logger.info("{}API Key criada id={} usuarioId={} prefixo={}", DOMAIN_PREFIX, salva.getId(), usuario.getId(), prefixo);

        return toCreatedDTO(salva, chave);
    }

    @Transactional(readOnly = true)
    public List<ApiKeyListDTO> listar(Usuario caller, Long usuarioId) {
        Long alvoId = resolverUsuarioAlvoListagem(caller, usuarioId);
        return apiKeyRepository.findByUsuarioIdOrderByDataCriacaoDesc(alvoId).stream()
                .map(this::toListDTO)
                .toList();
    }

    @Transactional
    public void revogar(Usuario caller, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new ApiKeyNotFoundException(apiKeyId));

        if (!podeGerenciarKey(caller, apiKey)) {
            throw new ApiKeyNotFoundException(apiKeyId);
        }

        if (!apiKey.isRevogado()) {
            apiKey.setRevogado(true);
            apiKeyRepository.save(apiKey);
            logger.info("{}API Key revogada id={} usuarioId={}", DOMAIN_PREFIX, apiKeyId, apiKey.getUsuario().getId());
        }
    }

    @Transactional
    public Optional<Usuario> autenticarPorChave(String chaveBruta) {
        if (chaveBruta == null || !chaveBruta.startsWith(CHAVE_PREFIX)) {
            return Optional.empty();
        }

        int prefixoLength = CHAVE_PREFIX.length() + PREFIXO_RANDOM_CHARS;
        if (chaveBruta.length() <= prefixoLength) {
            return Optional.empty();
        }

        String prefixo = chaveBruta.substring(0, prefixoLength);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByPrefixoAndRevogadoFalse(prefixo);
        if (apiKeyOpt.isEmpty()) {
            logger.debug("{}Autenticação API Key falhou prefixo={}", DOMAIN_PREFIX, prefixo);
            return Optional.empty();
        }

        ApiKey apiKey = apiKeyOpt.get();
        if (apiKey.isRevogado() || LocalDateTime.now(clock).isAfter(apiKey.getDataExpiracao())) {
            logger.debug("{}Autenticação API Key inválida prefixo={}", DOMAIN_PREFIX, prefixo);
            return Optional.empty();
        }

        if (!passwordEncoder.matches(chaveBruta, apiKey.getHashChave())) {
            logger.debug("{}Autenticação API Key inválida prefixo={}", DOMAIN_PREFIX, prefixo);
            return Optional.empty();
        }

        Usuario usuario = apiKey.getUsuario();
        if (!usuario.isEnabled() || !temPermissaoApiKey(usuario)) {
            logger.debug("{}Autenticação API Key negada usuarioId={} prefixo={}", DOMAIN_PREFIX, usuario.getId(), prefixo);
            return Optional.empty();
        }

        apiKey.setUltimoUsoEm(LocalDateTime.now(clock));
        apiKeyRepository.save(apiKey);

        return Optional.of(usuario);
    }

    public Usuario resolverUsuarioPorLogin(String login) {
        return usuarioRepository.findByLoginAndAtivoTrue(login)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado"));
    }

    private Long resolverUsuarioAlvoListagem(Usuario caller, Long usuarioId) {
        if (isAdmin(caller)) {
            return usuarioId != null ? usuarioId : caller.getId();
        }

        exigirPermissaoApiKey(caller);

        if (usuarioId != null && !usuarioId.equals(caller.getId())) {
            throw new ApiKeyNotFoundException();
        }

        return caller.getId();
    }

    private boolean podeGerenciarKey(Usuario caller, ApiKey apiKey) {
        if (isAdmin(caller)) {
            return true;
        }

        if (!temPermissaoApiKey(caller)) {
            return false;
        }

        return apiKey.getUsuario().getId().equals(caller.getId());
    }

    private void exigirPermissaoApiKey(Usuario usuario) {
        if (!temPermissaoApiKey(usuario)) {
            throw new AccessDeniedException("Permissão API_KEY necessária");
        }
    }

    private boolean temPermissaoApiKey(Usuario usuario) {
        List<String> permissoes = usuario.getPermissoes();
        return permissoes != null && permissoes.contains(PERMISSAO_API_KEY);
    }

    private boolean isAdmin(Usuario usuario) {
        List<String> permissoes = usuario.getPermissoes();
        return permissoes != null && permissoes.contains(PERMISSAO_ADMIN);
    }

    private String gerarPrefixo() {
        StringBuilder builder = new StringBuilder(CHAVE_PREFIX);
        for (int i = 0; i < PREFIXO_RANDOM_CHARS; i++) {
            int index = SECURE_RANDOM.nextInt(PREFIXO_CHARS.length());
            builder.append(PREFIXO_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private String gerarSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKeyCreatedDTO toCreatedDTO(ApiKey apiKey, String chave) {
        return new ApiKeyCreatedDTO(
                apiKey.getId(),
                apiKey.getNome(),
                apiKey.getPrefixo(),
                chave,
                apiKey.getDataExpiracao(),
                apiKey.getEscopo(),
                apiKey.getDataCriacao()
        );
    }

    private ApiKeyListDTO toListDTO(ApiKey apiKey) {
        return new ApiKeyListDTO(
                apiKey.getId(),
                apiKey.getNome(),
                apiKey.getPrefixo(),
                apiKey.getDataExpiracao(),
                apiKey.isRevogado(),
                apiKey.getEscopo(),
                apiKey.getUltimoUsoEm(),
                apiKey.getDataCriacao()
        );
    }
}
