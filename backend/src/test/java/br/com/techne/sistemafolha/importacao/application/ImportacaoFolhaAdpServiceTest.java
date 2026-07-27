package br.com.techne.sistemafolha.importacao.application;

import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioImportRef;
import br.com.techne.sistemafolha.cadastros.port.RubricaImportRef;
import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaDuplicadaException;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @InjectMocks
    private ImportacaoFolhaAdpService importacaoFolhaAdpService;

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
        verify(cadastrosImportLookupPort, never()).findFuncionarioByIdExterno(any());
    }

    @Test
    void importar_happyPath_chamaPersistirImportacao() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);

        FolhaPagamentoDTO dto = new FolhaPagamentoDTO(
            1L, 10L, "João", 2L, "001", "Salário", "PROVENTO",
            3L, "Analista", 4L, "CC", 5L, "LN",
            COMPETENCIA_INICIO, COMPETENCIA_FIM,
            new BigDecimal("1000"), BigDecimal.ONE, new BigDecimal("1000")
        );
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of(dto));

        List<FolhaPagamentoDTO> result = importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, false);

        assertEquals(1, result.size());
        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        FolhaImportacaoCommand command = captor.getValue();
        assertEquals(COMPETENCIA_INICIO, command.competenciaInicio());
        assertEquals(COMPETENCIA_FIM, command.competenciaFim());
        assertFalse(command.substituirExistente());
        assertFalse(command.decimoTerceiro());
        assertTrue(command.linhas().isEmpty());
    }

    @Test
    void importar_duplicidadeComConfirmacao_passaSubstituirExistente() throws Exception {
        MockMultipartFile arquivo = arquivoComCompetencia();
        when(folhaConsultaPort.existsResumoAtivo(eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), anyBoolean()))
            .thenReturn(true);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, true);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        assertTrue(captor.getValue().substituirExistente());
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
            eq("12345678901"), eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM)))
            .thenReturn(false);
        when(folhaImportacaoPort.persistirImportacao(any())).thenReturn(List.of());

        importacaoFolhaAdpService.importarFolhaAdp(arquivo, false, true);

        ArgumentCaptor<FolhaImportacaoCommand> captor = ArgumentCaptor.forClass(FolhaImportacaoCommand.class);
        verify(folhaImportacaoPort).persistirImportacao(captor.capture());
        FolhaImportacaoCommand command = captor.getValue();
        assertTrue(command.substituirExistente());
        assertFalse(command.linhas().isEmpty());
        assertEquals(1L, command.linhas().get(0).funcionarioId());
        assertEquals(2L, command.linhas().get(0).rubricaId());
        verify(folhaConsultaPort, never()).existsByFuncionarioIdAndRubricaIdAndPeriodo(
            any(), any(), any(), any());
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
}