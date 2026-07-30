package br.com.techne.sistemafolha.importacao.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
@Transactional
@EnabledIf("isDockerAvailable")
class ImportacaoFolhaAdpIntegrationTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ImportacaoFolhaAdpService importacaoFolhaAdpService;

    @Autowired
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private CentroCustoRepository centroCustoRepository;

    @BeforeEach
    void seedFuncionarioParaImportacao() {
        Cargo cargo = cargoRepository.findById(1L).orElseThrow();
        CentroCusto centroCusto = centroCustoRepository.findById(767L).orElseThrow();

        Funcionario funcionario = new Funcionario();
        funcionario.setNome("João da Silva");
        funcionario.setCpf("12345678901");
        funcionario.setDataAdmissao(LocalDate.of(2020, 1, 15));
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        funcionario.setIdExterno("12345");
        funcionario.setAtivo(true);
        funcionarioRepository.save(funcionario);
    }

    @Test
    void importar_fixtureMinimal_persistePeloMenosUmaLinha() throws Exception {
        MockMultipartFile arquivo = fixture("importacao/folha-adp-minimal.txt");

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        long linhasPersistidas = folhaPagamentoRepository
            .findByCompetenciaAndDecimoTerceiroAndAtivoTrue(COMPETENCIA_INICIO, COMPETENCIA_FIM, false)
            .size();

        assertTrue(linhasPersistidas >= 1, "expected at least one folha line persisted");
    }

    static boolean isDockerAvailable() {
        try {
            return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception ex) {
            return false;
        }
    }

    private MockMultipartFile fixture(String classpathLocation) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readAllBytes();
        }
        return new MockMultipartFile(
            "arquivo",
            resource.getFilename(),
            "text/plain",
            bytes
        );
    }
}
