package br.com.techne.sistemafolha.importacao.application;

import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioImportRef;
import br.com.techne.sistemafolha.cadastros.port.RubricaImportRef;
import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaDuplicadaException;
import br.com.techne.sistemafolha.folha.domain.FolhaProcessamentoFalhaException;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoPort;
import br.com.techne.sistemafolha.folha.port.FolhaProcessamentoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoFolhaAdpServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private CadastrosImportLookupPort cadastrosImportLookupPort;

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private FolhaImportacaoPort folhaImportacaoPort;

    @Mock
    private FolhaProcessamentoPort folhaProcessamentoPort;

    @InjectMocks
    private ImportacaoFolhaAdpService importacaoFolhaAdpService;

    @Test
    void importar_fixtureMinimal_happyPathPersisteZeroLinhas() throws Exception {
        MockMultipartFile arquivo = fixture("importacao/folha-adp-minimal.txt");
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        ImportacaoFolhaAdpResult result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        assertTrue(result.folhasPagamento().isEmpty());
        verify(folhaImportacaoPort).persistirImportacao(any());
        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_fixtureLayoutInvalido_funcionarioInexistente_lancaRuntimeException() throws Exception {
        MockMultipartFile arquivo = fixture("importacao/folha-adp-invalid.txt");
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("99999"))
            .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false)
        );

        assertTrue(ex.getMessage().contains("Funcionários não encontrados"));
        verify(folhaImportacaoPort, never()).persistirImportacao(any());
        verify(folhaProcessamentoPort, never()).processar(any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void importar_duplicidadeSemConfirmar_lancaFolhaDuplicadaENaoPersiste() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);

        FolhaDuplicadaException ex = assertThrows(
            FolhaDuplicadaException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false)
        );

        assertEquals(COMPETENCIA_INICIO.toString(), ex.getCompetenciaInicio());
        assertEquals(COMPETENCIA_FIM.toString(), ex.getCompetenciaFim());
        assertFalse(ex.isDecimoTerceiro());
        verify(folhaImportacaoPort, never()).persistirImportacao(any());
        verify(folhaProcessamentoPort, never()).processar(any(), any(), anyBoolean(), anyBoolean());
        verify(cadastrosImportLookupPort, never()).findFuncionarioByIdExterno(any());
    }

    @Test
    void importar_happyPath_chamaPersistirImportacaoEProcessamento() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);

        FolhaPagamentoDTO dto = new FolhaPagamentoDTO(
            1L, 10L, "João", 2L, "001", "Salário", "PROVENTO",
            3L, "Analista", 4L, "CC", 5L, "LN",
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            new BigDecimal("1000"), BigDecimal.ONE, new BigDecimal("1000"), false
        );
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of(dto));
        ProcessamentoResultadoDTO processamento = new ProcessamentoResultadoDTO(1, 2, 1);
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(processamento);

        ImportacaoFolhaAdpResult result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        assertEquals(1, result.folhasPagamento().size());
        assertEquals(processamento, result.processamento());
        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        FolhaImportacaoCommand command = captor.getValue();
        assertEquals(COMPETENCIA_INICIO, command.competenciaInicio());
        assertEquals(COMPETENCIA_FIM, command.competenciaFim());
        assertFalse(command.substituirExistente());
        assertFalse(command.decimoTerceiro());
        assertTrue(command.linhas().isEmpty());
        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_duplicidadeComConfirmacao_passaSubstituirExistenteEEncadeiaProcessamento() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), anyBoolean()))
            .thenReturn(true);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, true);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().substituirExistente());
        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_confirmarSubstituicao_naoPulaLinhasQuandoJaExistem() throws Exception {
        MockMultipartFile arquivo = arquivoComFuncionarioERubrica();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);

        FuncionarioImportRef funcionario = new FuncionarioImportRef(
            1L, "12345", "João", "12345678901", 3L, 4L, 5L);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionario));
        when(cadastrosImportLookupPort.findOrCreateRubrica(eq("0010"), anyString(), anyString()))
            .thenReturn(new RubricaImportRef(2L, "0010", "PROVENTO"));
        when(folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            eq("12345678901"), eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(1, 1, 1));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, true);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        FolhaImportacaoCommand command = captor.getValue();
        assertTrue(command.substituirExistente());
        assertFalse(command.linhas().isEmpty());
        assertEquals(1L, command.linhas().get(0).funcionarioId());
        assertEquals(2L, command.linhas().get(0).rubricaId());
        verify(folhaConsultaPort, never()).existsByFuncionarioIdAndRubricaIdAndPeriodo(
            any(), any(), any(), any(), anyBoolean());
        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_processamentoFalha_propagaFolhaProcessamentoFalhaException() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenThrow(new RuntimeException("Falha interna do motor"));

        FolhaProcessamentoFalhaException ex = assertThrows(
            FolhaProcessamentoFalhaException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false)
        );

        assertEquals("Falha interna do motor", ex.getMessage());
        verify(folhaImportacaoPort).persistirImportacao(any());
        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_processamentoZeroFichas_retornaResultadoComposto() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        ProcessamentoResultadoDTO processamentoZero = new ProcessamentoResultadoDTO(0, 0, 0);
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(processamentoZero);

        ImportacaoFolhaAdpResult result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        assertNotNull(result.processamento());
        assertEquals(0, result.processamento().totalFichas());
        assertEquals(0, result.processamento().totalLinhas());
        assertTrue(result.folhasPagamento().isEmpty());
    }

    @Test
    void importar_funcionarioNaoEncontrado_nuncaChamaProcessamento() throws Exception {
        MockMultipartFile arquivo = arquivoComFuncionarioInexistente();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("99999"))
            .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false)
        );

        assertTrue(ex.getMessage().contains("Funcionários não encontrados"));
        verify(folhaImportacaoPort, never()).persistirImportacao(any());
        verify(folhaProcessamentoPort, never()).processar(any(), any(), anyBoolean(), anyBoolean());
    }

    private MockMultipartFile fixture(String classpathLocation) throws IOException {
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

    private MockMultipartFile arquivoComCompetencia() {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n";
        return new MockMultipartFile(
            "arquivo",
            "folha.txt",
            "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252"))
        );
    }

    /**
     * ADP-like fixed-width: competência + linha de admissão + linha de rubrica.
     * Positions: idExterno [50,55), nome [57,96), "Admiss" [96,102);
     * rubrica [0,32), valores [32,65).
     */
    private MockMultipartFile arquivoComFuncionarioERubrica() {
        StringBuilder admissao = new StringBuilder();
        while (admissao.length() < 50) {
            admissao.append(' ');
        }
        admissao.append("12345");
        while (admissao.length() < 57) {
            admissao.append(' ');
        }
        admissao.append("JOAO DA SILVA");
        while (admissao.length() < 96) {
            admissao.append(' ');
        }
        admissao.append("Admiss");
        admissao.append("ao"); // length must be > 102 for the parser gate

        String rubrica = String.format("%-32s", "0010 SALARIO BASE")
            + String.format("%-33s", "1,00 1000,00 1000,00+");

        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + admissao + "\n"
            + rubrica + "\n";
        return new MockMultipartFile(
            "arquivo",
            "folha.txt",
            "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252"))
        );
    }

    private MockMultipartFile arquivoComFuncionarioInexistente() {
        StringBuilder admissao = new StringBuilder();
        while (admissao.length() < 50) {
            admissao.append(' ');
        }
        admissao.append("99999");
        while (admissao.length() < 57) {
            admissao.append(' ');
        }
        admissao.append("FUNCIONARIO INEXISTENTE");
        while (admissao.length() < 96) {
            admissao.append(' ');
        }
        admissao.append("Admiss");
        admissao.append("ao");

        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + admissao + "\n";
        return new MockMultipartFile(
            "arquivo",
            "folha.txt",
            "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252"))
        );
    }
}
