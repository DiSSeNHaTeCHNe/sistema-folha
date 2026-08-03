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
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoLinhaCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoPort;
import br.com.techne.sistemafolha.folha.port.FolhaProcessamentoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private ImportacaoFolhaAdpService importacaoFolhaAdpService;

    @Test
    void importar_fixtureMinimal_happyPathPersisteLinha() throws Exception {
        MockMultipartFile arquivo = fixture("importacao/folha-adp-minimal.txt");
        FuncionarioImportRef funcionario = new FuncionarioImportRef(
            1L, "12345", "João", "12345678901", 3L, 4L, 5L);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionario));
        when(cadastrosImportLookupPort.findOrCreateRubrica(eq("0010"), anyString(), anyString()))
            .thenReturn(new RubricaImportRef(2L, "0010", "PROVENTO"));
        when(folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            eq("12345678901"), eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(false);
        when(folhaConsultaPort.existsByFuncionarioIdAndRubricaIdAndPeriodo(
            eq(1L), eq(2L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(false);
        FolhaPagamentoDTO dto = new FolhaPagamentoDTO(
            1L, 10L, "João", 2L, "0010", "Salário", "PROVENTO",
            3L, "Analista", 4L, "CC", 5L, "LN",
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            new BigDecimal("1000"), BigDecimal.ONE, new BigDecimal("1000"), false
        );
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of(dto));
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(1, 1, 1));

        ImportacaoFolhaAdpResult result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        assertEquals(1, result.folhasPagamento().size());
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
    void importar_comTotaisNoArquivo_montaResumoNoCommand() throws Exception {
        MockMultipartFile arquivo = arquivoComTotaisResumo();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(1, 1, 1));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertNotNull(captor.getValue().resumo());
        assertEquals(1, captor.getValue().resumo().totalEmpregados());
    }

    @Test
    void importar_decimoTerceiroDetectadoPorCompetenciaDezembro() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetenciaDezembro();
        when(folhaConsultaPort.existsResumoAtivo(
            eq(LocalDate.of(2024, 12, 1)), eq(LocalDate.of(2024, 12, 31)), eq(true)))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(LocalDate.of(2024, 12, 1)), eq(LocalDate.of(2024, 12, 31)), eq(true), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, null, false);

        verify(folhaProcessamentoPort).processar(
            LocalDate.of(2024, 12, 1), LocalDate.of(2024, 12, 31), true, false);
    }

    @Test
    void importar_conflitoCpfCompetencia_lancaRuntimeException() throws Exception {
        MockMultipartFile arquivo = arquivoComFuncionarioERubrica();
        FuncionarioImportRef funcionario = new FuncionarioImportRef(
            1L, "12345", "João", "12345678901", 3L, 4L, 5L);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionario));
        when(cadastrosImportLookupPort.findOrCreateRubrica(eq("0010"), anyString(), anyString()))
            .thenReturn(new RubricaImportRef(2L, "0010", "PROVENTO"));
        when(folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            eq("12345678901"), eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(true);

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false)
        );

        assertTrue(ex.getMessage().contains("Folha duplicada por CPF"));
        verify(folhaImportacaoPort, never()).persistirImportacao(any());
    }

    @Test
    void importar_linhaJaExistente_naoDuplicaNoCommand() throws Exception {
        MockMultipartFile arquivo = arquivoComFuncionarioERubrica();
        FuncionarioImportRef funcionario = new FuncionarioImportRef(
            1L, "12345", "João", "12345678901", 3L, 4L, 5L);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionario));
        when(cadastrosImportLookupPort.findOrCreateRubrica(eq("0010"), anyString(), anyString()))
            .thenReturn(new RubricaImportRef(2L, "0010", "PROVENTO"));
        when(folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            eq("12345678901"), eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(false);
        when(folhaConsultaPort.existsByFuncionarioIdAndRubricaIdAndPeriodo(
            eq(1L), eq(2L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false)))
            .thenReturn(true);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
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

    @Test
    void importar_decimoTerceiroExplicito_usaFlagInformada() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, true))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(true), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, true, false);

        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, true, false);
    }

    @Test
    void importar_duplicidadeDecimoTerceiro_lancaFolhaDuplicada() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetenciaDezembro();
        when(folhaConsultaPort.existsResumoAtivo(
            eq(LocalDate.of(2024, 12, 1)), eq(LocalDate.of(2024, 12, 31)), eq(true)))
            .thenReturn(true);

        FolhaDuplicadaException ex = assertThrows(
            FolhaDuplicadaException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, null, false));

        assertTrue(ex.isDecimoTerceiro());
        assertTrue(ex.getMessage().contains("13º salário"));
    }

    @Test
    void importar_ignoraLinhasEvtESeparador() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Evt linha de evento ignorada\n"
            + "¯¯¯¯ separador\n"
            + linhaAdmissao("12345", "JOAO") + "\n"
            + linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+") + "\n";
        MockMultipartFile arquivo = arquivoBytes(conteudo);
        stubImportacaoHappyPath(funcionarioPadrao());

        ImportacaoFolhaAdpResult result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(1, captor.getValue().linhas().size());
    }

    @Test
    void importar_segundaRubricaNaMesmaLinha_persisteDuasLinhas() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaDuplaRubrica() + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());
        when(cadastrosImportLookupPort.findOrCreateRubrica(anyString(), anyString(), anyString()))
            .thenAnswer(inv -> {
                String codigo = inv.getArgument(0);
                if ("0020".equals(codigo)) {
                    return new RubricaImportRef(3L, "0020", "DESCONTO");
                }
                return new RubricaImportRef(2L, "0010", "PROVENTO");
            });
        when(folhaConsultaPort.existsByFuncionarioIdAndRubricaIdAndPeriodo(
            any(), any(), any(), any(), anyBoolean())).thenReturn(false);

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(2, captor.getValue().linhas().size());
    }

    @Test
    void importar_segundaRubricaSemValores_pulaSegunda() throws Exception {
        String linha = linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+");
        while (linha.length() < 131) {
            linha += " ";
        }
        linha = linha.substring(0, 66) + String.format("%-31s", "0020 DESCONTO") + "   ";
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linha + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(1, captor.getValue().linhas().size());
    }

    @Test
    void importar_rubricaDesconto_mapeiaTipoDesconto() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("0020 INSS", "1,00 100,00 100,00-") + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        verify(cadastrosImportLookupPort).findOrCreateRubrica(eq("0020"), anyString(), eq("DESCONTO"));
    }

    @Test
    void importar_rubricaInformativaSemSinal_mapeiaInformativo() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("0030 INFO", "1,00 100,00 100,00") + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        verify(cadastrosImportLookupPort).findOrCreateRubrica(eq("0030"), anyString(), eq("INFORMATIVO"));
    }

    @Test
    void importar_rubricaLinhaCurta_ignoraLinha() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + "0010 CURTA\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_rubricaFormatoInvalido_ignoraLinha() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("INVALIDA", "1,00 100,00 100,00+") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_valoresSemMatchNumerico_ignoraLinha() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("0010 SALARIO", "sem numeros aqui") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_rubricaSemFuncionarioVinculado_naoPersisteLinha() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+") + "\n");
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_resumoParcial_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\nTotal de Empregados: 1\n";
        MockMultipartFile arquivo = arquivoBytes(conteudo);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(null, captor.getValue().resumo());
    }

    @Test
    void importar_processamentoNullMessage_encapsulaFolhaProcessamentoFalhaException() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenThrow(new RuntimeException((String) null));

        FolhaProcessamentoFalhaException ex = assertThrows(
            FolhaProcessamentoFalhaException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false));

        assertEquals("RuntimeException", ex.getMessage());
    }

    @Test
    void importar_processamentoFolhaProcessamentoFalhaException_repassa() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenThrow(new FolhaProcessamentoFalhaException("falha direta", null));

        FolhaProcessamentoFalhaException ex = assertThrows(
            FolhaProcessamentoFalhaException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false));

        assertEquals("falha direta", ex.getMessage());
    }

    @Test
    void parseBigDecimal_casosLimite() throws Exception {
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal(null));
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("   "));
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("."));
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("abc"));
        assertEquals(new BigDecimal("1234.56"), invokeParseBigDecimal("1.234,56+"));
    }

    @Test
    void determinarTipoRubrica_todosRamos() throws Exception {
        assertEquals("INFORMATIVO", invokeDeterminarTipo(null));
        assertEquals("INFORMATIVO", invokeDeterminarTipo(""));
        assertEquals("PROVENTO", invokeDeterminarTipo("+"));
        assertEquals("DESCONTO", invokeDeterminarTipo("-"));
        assertEquals("INFORMATIVO", invokeDeterminarTipo("X"));
    }

    @Test
    void importar_linhaComprimento130_aplicaPadding() throws Exception {
        String rubrica = linhaComprimentoExato(130);
        assertEquals(130, rubrica.length());
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + rubrica + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(1, captor.getValue().linhas().size());
    }

    private void stubImportacaoHappyPath(FuncionarioImportRef funcionario) {
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionario));
        when(cadastrosImportLookupPort.findOrCreateRubrica(anyString(), anyString(), anyString()))
            .thenAnswer(inv -> new RubricaImportRef(2L, inv.getArgument(0), "PROVENTO"));
        when(folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            anyString(), any(), any(), any(), anyBoolean())).thenReturn(false);
        when(folhaConsultaPort.existsByFuncionarioIdAndRubricaIdAndPeriodo(
            any(), any(), any(), any(), anyBoolean())).thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(1, 1, 1));
    }

    private FuncionarioImportRef funcionarioPadrao() {
        return new FuncionarioImportRef(1L, "12345", "João", "12345678901", 3L, 4L, 5L);
    }

    private String linhaAdmissao(String idExterno, String nome) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 50) {
            sb.append(' ');
        }
        sb.append(idExterno);
        while (sb.length() < 57) {
            sb.append(' ');
        }
        sb.append(nome);
        while (sb.length() < 96) {
            sb.append(' ');
        }
        sb.append("Admissao");
        return sb.toString();
    }

    private String linhaRubricaSimples(String rubrica, String valores) {
        return String.format("%-32s", rubrica) + String.format("%-33s", valores);
    }

    private String linhaDuplaRubrica() {
        String line = String.format("%-32s", "0010 SALARIO BASE")
            + String.format("%-33s", "1,00 1000,00 1000,00+")
            + String.format("%-31s", "0020 DESCONTO INSS")
            + String.format("%-33s", "1,00 100,00 100,00-");
        if (line.length() <= 130) {
            line = String.format("%-131s", line);
        }
        return line;
    }

    private String linhaComprimentoExato(int tamanho) {
        String base = linhaRubricaSimples("0010 SALARIO BASE", "1,00 1000,00 1000,00+");
        if (base.length() >= tamanho) {
            return base.substring(0, tamanho);
        }
        return String.format("%-" + tamanho + "s", base);
    }

    private void stubImportacaoSemProcessarRubricas() {
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionarioPadrao()));
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));
    }

    private MockMultipartFile arquivoBytes(String conteudo) {
        return new MockMultipartFile(
            "arquivo", "folha.txt", "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252")));
    }

    private BigDecimal invokeParseBigDecimal(String valor) throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod("parseBigDecimal", String.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(importacaoFolhaAdpService, valor);
    }

    private String invokeDeterminarTipo(String tipo) throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod("determinarTipoRubricaDescricao", String.class);
        method.setAccessible(true);
        return (String) method.invoke(importacaoFolhaAdpService, tipo);
    }

    @Test
    void importar_decimoTerceiroNull_competenciaOutubro_usaFalse() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, null, false);

        verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);
    }

    @Test
    void importar_linhaSemPadraoRubrica_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + "    x\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_totaisInvalidos_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: xyz\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Pagamentos: 1000,00\n"
            + "Total de Descontos: 200,00\n"
            + "Total Líquido: 800,00\n";
        MockMultipartFile arquivo = arquivoBytes(conteudo);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(null, captor.getValue().resumo());
    }

    @Test
    void importar_competenciaMalformada_naoQuebraImportacao() throws Exception {
        MockMultipartFile arquivo = arquivoBytes("Competência: formato-invalido\n");
        when(folhaConsultaPort.existsResumoAtivo(any(), any(), anyBoolean())).thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(any(), any(), anyBoolean(), anyBoolean()))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        assertNotNull(importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false));
    }

    @Test
    void importar_rubricaComValoresVazios_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + String.format("%-32s", "0010 SALARIO") + String.format("%-33s", "   ") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void parseBigDecimal_apenasSinal_retornaZero() throws Exception {
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("-"));
    }

    @Test
    void extrairPeriodo_competenciaValida() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        LocalDate[] periodo = invokeExtrairPeriodo(arquivo);
        assertEquals(COMPETENCIA_INICIO, periodo[0]);
        assertEquals(COMPETENCIA_FIM, periodo[1]);
    }

    @Test
    void importar_linhaComQuintoCharNaoBlank_ignoraComoRubrica() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + "1234567890123456789012345678901234567890\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_linhaSeparadorAdp_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + "¯¯¯¯ separador\n");
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        verify(folhaImportacaoPort).persistirImportacao(any());
    }

    @Test
    void importar_segundaRubricaSemTexto_pulaSegunda() throws Exception {
        String linha = linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+");
        linha = String.format("%-131s", linha);
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linha + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(1, captor.getValue().linhas().size());
    }

    @Test
    void importar_rubricaSemDescricao_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("0010", "1,00 1000,00 1000,00+") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void importar_rubricaSemCodigo_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + String.format("%-32s", "   ") + String.format("%-33s", "1,00 1000,00 1000,00+") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void extrairPeriodo_competenciaIncompleta() throws Exception {
        LocalDate[] periodo = invokeExtrairPeriodo(arquivoBytes("Competência: 01/10/2024\n"));
        assertNotNull(periodo[0]);
        assertNotNull(periodo[1]);
    }

    @Test
    void extrairPeriodo_competenciaSemDatas() throws Exception {
        LocalDate[] periodo = invokeExtrairPeriodo(arquivoBytes("Competência:\n"));
        assertNotNull(periodo[0]);
    }

    @Test
    void importar_resumoFaltandoLiquido_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: 1\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Pagamentos: 1000,00\n"
            + "Total de Descontos: 200,00\n";
        importarResumoParcial(arquivoBytes(conteudo));
    }

    @Test
    void importar_resumoFaltandoDescontos_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: 1\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Pagamentos: 1000,00\n"
            + "Total Líquido: 800,00\n";
        importarResumoParcial(arquivoBytes(conteudo));
    }

    @Test
    void importar_resumoFaltandoPagamentos_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: 1\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Descontos: 200,00\n"
            + "Total Líquido: 800,00\n";
        importarResumoParcial(arquivoBytes(conteudo));
    }

    @Test
    void parseBigDecimal_apenasMais_retornaZero() throws Exception {
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("+"));
    }

    @Test
    void extrairPeriodo_arquivoSemCompetencia_usaFallback() throws Exception {
        LocalDate[] periodo = invokeExtrairPeriodo(arquivoBytes("Sem cabecalho\n"));
        assertNotNull(periodo[0]);
    }

    @Test
    void importar_ioExceptionDuranteLeitura_encapsulaRuntimeException() throws Exception {
        MultipartFile arquivo = org.mockito.Mockito.mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("folha.txt");
        when(arquivo.getSize()).thenReturn(10L);
        when(arquivo.getInputStream())
            .thenReturn(new java.io.ByteArrayInputStream(
                "Competência: 01/10/2024 a 31/10/2024\n".getBytes(Charset.forName("WINDOWS-1252"))))
            .thenThrow(new IOException("falha leitura"));
        when(folhaConsultaPort.existsResumoAtivo(any(), any(), anyBoolean())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false));

        assertTrue(ex.getMessage().contains("Erro durante importação"));
    }

    @Test
    void importar_findOrCreateFalha_continuaSemLinha() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+") + "\n");
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(cadastrosImportLookupPort.findFuncionarioByIdExterno("12345"))
            .thenReturn(Optional.of(funcionarioPadrao()));
        when(cadastrosImportLookupPort.findOrCreateRubrica(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("falha rubrica"));
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void extrairPeriodo_dataInvalida_caiNoCatch() throws Exception {
        LocalDate[] periodo = invokeExtrairPeriodo(
            arquivoBytes("Competência: 99/99/9999 a 99/99/9999\n"));
        assertNotNull(periodo[0]);
    }

    @Test
    void importar_segundaRubricaSemValores_pulaSegundaColuna() throws Exception {
        String base = linhaRubricaSimples("0010 SALARIO", "1,00 1000,00 1000,00+");
        StringBuilder sb = new StringBuilder(base);
        while (sb.length() < 66) {
            sb.append(' ');
        }
        sb.append(String.format("%-31s", "0020 DESCONTO"));
        while (sb.length() < 131) {
            sb.append(' ');
        }
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + sb + "\n");
        stubImportacaoHappyPath(funcionarioPadrao());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(1, captor.getValue().linhas().size());
    }

    @Test
    void importar_linhaSeparadorMacron_ignora() throws Exception {
        byte[] header = "Competência: 01/10/2024 a 31/10/2024\n".getBytes(Charset.forName("WINDOWS-1252"));
        byte[] separador = new byte[] {(byte) 0xAF, (byte) 0xAF, (byte) 0xAF, (byte) 0xAF, ' ', 'X', 'y', '\n'};
        byte[] conteudo = new byte[header.length + separador.length];
        System.arraycopy(header, 0, conteudo, 0, header.length);
        System.arraycopy(separador, 0, conteudo, header.length, separador.length);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "folha.txt", "text/plain", conteudo);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        verify(folhaImportacaoPort).persistirImportacao(any());
    }

    @Test
    void importar_rubricaEmBrancoComValores_ignora() throws Exception {
        MockMultipartFile arquivo = arquivoBytes(
            "Competência: 01/10/2024 a 31/10/2024\n"
                + linhaAdmissao("12345", "JOAO") + "\n"
                + String.format("%-32s", "   ") + String.format("%-33s", "1,00 1000,00 1000,00+") + "\n");
        stubImportacaoSemProcessarRubricas();

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().linhas().isEmpty());
    }

    @Test
    void parseBigDecimal_apenasTracos_retornaZero() throws Exception {
        assertEquals(BigDecimal.ZERO, invokeParseBigDecimal("---"));
    }

    @Test
    void importar_totalEmpregadosOverflow_naoMontaResumo() throws Exception {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: 999999999999999999999\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Pagamentos: 1000,00\n"
            + "Total de Descontos: 200,00\n"
            + "Total Líquido: 800,00\n";
        importarResumoParcial(arquivoBytes(conteudo));
    }

    @Test
    void processarRubrica_funcionarioNull_naoAdicionaLinha() throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod(
            "processarRubrica",
            String.class, int.class, int.class, int.class, int.class,
            FuncionarioImportRef.class, LocalDate.class, LocalDate.class,
            List.class, Set.class, boolean.class, boolean.class);
        method.setAccessible(true);
        List<FolhaImportacaoLinhaCommand> linhas = new ArrayList<>();
        method.invoke(importacaoFolhaAdpService,
            linhaRubricaSimples("0010 SAL", "1,00 1000,00 1000,00+"),
            0, 31, 32, 65, null,
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            linhas, new LinkedHashSet<>(), false, false);

        assertTrue(linhas.isEmpty());
    }

    @Test
    void processarRubrica_indicesInvalidos_capturaStringIndexOutOfBounds() throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod(
            "processarRubrica",
            String.class, int.class, int.class, int.class, int.class,
            FuncionarioImportRef.class, LocalDate.class, LocalDate.class,
            List.class, Set.class, boolean.class, boolean.class);
        method.setAccessible(true);
        List<FolhaImportacaoLinhaCommand> linhas = new ArrayList<>();
        method.invoke(importacaoFolhaAdpService,
            "x".repeat(70),
            0, 80, 32, 65, funcionarioPadrao(),
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            linhas, new LinkedHashSet<>(), false, false);

        assertTrue(linhas.isEmpty());
    }

    @Test
    void processarRubrica_linhaCurta_naoAdicionaLinha() throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod(
            "processarRubrica",
            String.class, int.class, int.class, int.class, int.class,
            FuncionarioImportRef.class, LocalDate.class, LocalDate.class,
            List.class, Set.class, boolean.class, boolean.class);
        method.setAccessible(true);
        List<FolhaImportacaoLinhaCommand> linhas = new ArrayList<>();
        method.invoke(importacaoFolhaAdpService,
            "123456",
            0, 31, 32, 65, funcionarioPadrao(),
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            linhas, new LinkedHashSet<>(), false, false);

        assertTrue(linhas.isEmpty());
    }

    @Test
    void processarRubrica_formatoInvalido_naoAdicionaLinha() throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod(
            "processarRubrica",
            String.class, int.class, int.class, int.class, int.class,
            FuncionarioImportRef.class, LocalDate.class, LocalDate.class,
            List.class, Set.class, boolean.class, boolean.class);
        method.setAccessible(true);
        List<FolhaImportacaoLinhaCommand> linhas = new ArrayList<>();
        method.invoke(importacaoFolhaAdpService,
            linhaRubricaSimples("0010", "1,00 1000,00 1000,00+"),
            0, 31, 32, 65, funcionarioPadrao(),
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            linhas, new LinkedHashSet<>(), false, false);

        assertTrue(linhas.isEmpty());
    }

    private void importarResumoParcial(MockMultipartFile arquivo) throws Exception {
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());
        when(folhaProcessamentoPort.processar(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(false)))
            .thenReturn(new ProcessamentoResultadoDTO(0, 0, 0));

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertEquals(null, captor.getValue().resumo());
    }

    private LocalDate[] invokeExtrairPeriodo(MockMultipartFile arquivo) throws Exception {
        var method = ImportacaoFolhaAdpService.class.getDeclaredMethod(
            "extrairPeriodoCompetencia", org.springframework.web.multipart.MultipartFile.class);
        method.setAccessible(true);
        return (LocalDate[]) method.invoke(importacaoFolhaAdpService, arquivo);
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

    private MockMultipartFile arquivoComTotaisResumo() {
        String conteudo = "Competência: 01/10/2024 a 31/10/2024\n"
            + "Total de Empregados: 1\n"
            + "Total de Encargos: 100,00\n"
            + "Total de Pagamentos: 1000,00\n"
            + "Total de Descontos: 200,00\n"
            + "Total Líquido: 800,00\n";
        return new MockMultipartFile(
            "arquivo",
            "folha.txt",
            "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252"))
        );
    }

    private MockMultipartFile arquivoComCompetenciaDezembro() {
        String conteudo = "Competência: 01/12/2024 a 31/12/2024\n";
        return new MockMultipartFile(
            "arquivo",
            "folha-dez.txt",
            "text/plain",
            conteudo.getBytes(Charset.forName("WINDOWS-1252"))
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
