package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.api.ImportacaoResultadoDTO;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalDuplicadaException;
import br.com.techne.sistemafolha.beneficios.domain.ImportacaoBeneficioMensalInvalidaException;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import br.com.techne.sistemafolha.beneficios.infrastructure.TipoBeneficioRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoBeneficioMensalServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private TipoBeneficioRepository tipoBeneficioRepository;

    @Mock
    private BeneficioMensalRepository beneficioMensalRepository;

    @InjectMocks
    private ImportacaoBeneficioMensalService importacaoBeneficioMensalService;

    private Funcionario funcionario;
    private TipoBeneficio tipoBeneficio;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");
        funcionario.setCpf("12345678901");

        tipoBeneficio = new TipoBeneficio();
        tipoBeneficio.setId(10L);
        tipoBeneficio.setCodigo("5612");
        tipoBeneficio.setDescricao("Vale Refeição - Custo Empresa");
        tipoBeneficio.setAtivo(true);
    }

    @Test
    void importar_processa_linhas_validas() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
                linha("12345678901", "João Silva", "Vale Refeição - Custo Empresa", "5612", 150.50),
                linha("98765432100", "Maria Souza", "Seguros - Custo Empresa", "5322", 80.00)
        );

        Funcionario maria = new Funcionario();
        maria.setId(2L);
        maria.setNome("Maria Souza");
        maria.setCpf("98765432100");

        TipoBeneficio seguros = new TipoBeneficio();
        seguros.setId(11L);
        seguros.setCodigo("5322");
        seguros.setDescricao("Seguros - Custo Empresa");
        seguros.setAtivo(true);

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("98765432100")).thenReturn(Optional.of(maria));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5322")).thenReturn(Optional.of(seguros));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
                arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(2, resultado.processadas());
        assertEquals(0, resultado.erros());
        assertEquals(new BigDecimal("230.50"), resultado.totalValor());
        assertTrue(resultado.detalhesErros().isEmpty());
        verify(beneficioMensalRepository, times(2)).save(any(BeneficioMensal.class));
        verify(beneficioMensalRepository, never()).deleteByCompetenciaInicioAndCompetenciaFim(any(), any());
    }

    @Test
    void importar_rejeita_arquivo_com_erros_sem_persistir() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
                linha("12345678901", "João Silva", "Vale Refeição - Custo Empresa", "5612", 100),
                linha("00000000000", "Desconhecido", "Seguros - Custo Empresa", "5322", 50),
                linha("12345678901", "João Silva", "Tipo inválido", "9999", 20),
                linha("12345678901", "João Silva", "Valor negativo", "5612", -10)
        );

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("00000000000")).thenReturn(Optional.empty());
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("9999")).thenReturn(Optional.empty());

        ImportacaoBeneficioMensalInvalidaException ex = assertThrows(
                ImportacaoBeneficioMensalInvalidaException.class,
                () -> importacaoBeneficioMensalService.importar(
                        arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));

        assertEquals(3, ex.getDetalhesErros().size());
        assertTrue(ex.getMessage().contains("Nenhum registro foi salvo"));
        verify(beneficioMensalRepository, never()).save(any(BeneficioMensal.class));
        verify(beneficioMensalRepository, never()).deleteByCompetenciaInicioAndCompetenciaFim(any(), any());
    }

    @Test
    void importar_com_confirmacao_nao_remove_registros_quando_arquivo_tem_erros() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
                linha("00000000000", "Desconhecido", "Seguros - Custo Empresa", "5322", 50)
        );

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(true);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("00000000000")).thenReturn(Optional.empty());

        assertThrows(
                ImportacaoBeneficioMensalInvalidaException.class,
                () -> importacaoBeneficioMensalService.importar(
                        arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, true));

        verify(beneficioMensalRepository, never()).save(any(BeneficioMensal.class));
        verify(beneficioMensalRepository, never()).deleteByCompetenciaInicioAndCompetenciaFim(any(), any());
    }

    @Test
    void importar_lanca_excecao_quando_competencia_duplicada_sem_confirmacao() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
                linha("12345678901", "João Silva", "Vale Refeição - Custo Empresa", "5612", 100)
        );

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(true);

        BeneficioMensalDuplicadaException ex = assertThrows(
                BeneficioMensalDuplicadaException.class,
                () -> importacaoBeneficioMensalService.importar(
                        arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));

        assertTrue(ex.getMessage().contains("Já existem lançamentos"));
        assertEquals(COMPETENCIA_INICIO.toString(), ex.getCompetenciaInicio());
        assertEquals(COMPETENCIA_FIM.toString(), ex.getCompetenciaFim());
        verify(beneficioMensalRepository, never()).save(any());
        verify(beneficioMensalRepository, never()).deleteByCompetenciaInicioAndCompetenciaFim(any(), any());
    }

    @Test
    void importar_com_confirmacao_remove_registros_anteriores_e_insere_novos() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
                linha("12345678901", "João Silva", "Vale Refeição - Custo Empresa", "5612", 200)
        );

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(true);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
                arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, true);

        assertEquals(1, resultado.processadas());
        assertEquals(0, resultado.erros());
        verify(beneficioMensalRepository).deleteByCompetenciaInicioAndCompetenciaFim(
                COMPETENCIA_INICIO, COMPETENCIA_FIM);

        ArgumentCaptor<BeneficioMensal> captor = ArgumentCaptor.forClass(BeneficioMensal.class);
        verify(beneficioMensalRepository).save(captor.capture());
        BeneficioMensal salvo = captor.getValue();
        assertEquals(funcionario.getId(), salvo.getFuncionario().getId());
        assertEquals(tipoBeneficio.getId(), salvo.getTipoBeneficio().getId());
        assertEquals(new BigDecimal("200.00"), salvo.getValor());
        assertEquals(COMPETENCIA_INICIO, salvo.getCompetenciaInicio());
        assertEquals(COMPETENCIA_FIM, salvo.getCompetenciaFim());
    }

    @Test
    void importar_rejeita_arquivo_sem_aba_planilha1() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("OutraAba");
            MockMultipartFile arquivo = toMultipart(workbook);

            when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                    COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> importacaoBeneficioMensalService.importar(
                            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));

            assertTrue(ex.getMessage().contains("Planilha1"));
            verify(beneficioMensalRepository, never()).save(any());
        }
    }

    @Test
    void importar_arquivoNull_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            importacaoBeneficioMensalService.importar(null, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_arquivoVazio_lancaIllegalArgumentException() {
        MockMultipartFile vazio = new MockMultipartFile("file", new byte[0]);
        assertThrows(IllegalArgumentException.class, () ->
            importacaoBeneficioMensalService.importar(vazio, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_competenciaNull_lancaIllegalArgumentException() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
            linha("12345678901", "João Silva", "Vale", "5612", 100));
        assertThrows(IllegalArgumentException.class, () ->
            importacaoBeneficioMensalService.importar(arquivo, null, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_pulaLinhaComCpfVazio() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("Sem CPF");
            row.createCell(2).setCellValue("");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Desc");
            row.createCell(13).setCellValue(100.0);
        }, row -> {
            row.createCell(1).setCellValue("João Silva");
            row.createCell(2).setCellValue("12345678901");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setCellValue(150.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(1, resultado.processadas());
    }

    @Test
    void importar_codigoVazio_registraErro() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("João Silva");
            row.createCell(2).setCellValue("12345678901");
            row.createCell(8).setCellValue("");
            row.createCell(9).setCellValue("Sem código");
            row.createCell(13).setCellValue(100.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));

        assertThrows(ImportacaoBeneficioMensalInvalidaException.class, () ->
            importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_valorTextoComVirgula() throws IOException {
        MockMultipartFile arquivo = workbookComValorTexto("12345678901", "João Silva", "Vale", "5612", "R$ 1.234,56");
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(new BigDecimal("1234.56"), resultado.totalValor());
    }

    @Test
    void importar_valorInvalido_registraErro() throws IOException {
        MockMultipartFile arquivo = workbookComValorTexto(
            "12345678901", "João Silva", "Vale", "5612", "abc");
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));

        assertThrows(ImportacaoBeneficioMensalInvalidaException.class, () ->
            importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_valorBlank_registraErro() throws IOException {
        MockMultipartFile arquivo = workbookComValorTexto(
            "12345678901", "João Silva", "Vale", "5612", "");
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));

        assertThrows(ImportacaoBeneficioMensalInvalidaException.class, () ->
            importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
    }

    @Test
    void importar_semDescricao_naoSetaObservacao() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
            linha("12345678901", "João Silva", "", "5612", 100)
        );
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        ArgumentCaptor<BeneficioMensal> captor = ArgumentCaptor.forClass(BeneficioMensal.class);
        verify(beneficioMensalRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getObservacao());
    }

    private MockMultipartFile workbookComValorTexto(
            String cpf, String nome, String descricao, String codigo, String valorTexto) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            sheet.createRow(0);
            var row = sheet.createRow(1);
            row.createCell(1).setCellValue(nome);
            row.createCell(2).setCellValue(cpf);
            row.createCell(8).setCellValue(codigo);
            row.createCell(9).setCellValue(descricao);
            row.createCell(13).setCellValue(valorTexto);
            return toMultipart(workbook);
        }
    }

    @Test
    void importar_competenciaFimNull_lancaIllegalArgumentException() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
            linha("12345678901", "João Silva", "Vale", "5612", 100));
        assertThrows(IllegalArgumentException.class, () ->
            importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, null, false));
    }

    @Test
    void importar_pulaRowNull() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            sheet.createRow(0);
            sheet.createRow(2);
            var row = sheet.createRow(2);
            row.createCell(1).setCellValue("João Silva");
            row.createCell(2).setCellValue("12345678901");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setCellValue(100.0);
            MockMultipartFile arquivo = toMultipart(workbook);
            when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                    COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
            when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
            when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
            when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

            assertEquals(1, importacaoBeneficioMensalService.importar(
                arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false).processadas());
        }
    }

    @Test
    void importar_cpfCurto_normalizaComPadding() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("João");
            row.createCell(2).setCellValue("123456789");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setCellValue(100.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("00123456789")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false).processadas());
    }

    @Test
    void importar_erroFuncionarioSemNome_formataErro() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue("00000000000");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setCellValue(100.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("00000000000")).thenReturn(Optional.empty());

        ImportacaoBeneficioMensalInvalidaException ex = assertThrows(
            ImportacaoBeneficioMensalInvalidaException.class, () ->
                importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
        assertTrue(ex.getDetalhesErros().get(0).contains("00000000000"));
    }

    @Test
    void importar_valorCellBlank_registraErro() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            sheet.createRow(0);
            var row = sheet.createRow(1);
            row.createCell(1).setCellValue("João Silva");
            row.createCell(2).setCellValue("12345678901");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setBlank();
            MockMultipartFile arquivo = toMultipart(workbook);
            when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                    COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
            when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
            when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));

            assertThrows(ImportacaoBeneficioMensalInvalidaException.class, () ->
                importacaoBeneficioMensalService.importar(arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false));
        }
    }

    @Test
    void importar_cpfSomenteNaoDigitos_pulaLinha() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("X");
            row.createCell(2).setCellValue("abc");
            row.createCell(8).setCellValue("5612");
            row.createCell(13).setCellValue(100.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);

        ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(0, resultado.processadas());
    }

    @Test
    void importar_celulasNull_trataComoVazio() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            sheet.createRow(0);
            sheet.createRow(1);
            MockMultipartFile arquivo = toMultipart(workbook);
            when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                    COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);

            assertEquals(0, importacaoBeneficioMensalService.importar(
                arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false).processadas());
        }
    }

    @Test
    void importar_codigoNumericoComoTexto() throws IOException {
        MockMultipartFile arquivo = workbookComLinhaManual(row -> {
            row.createCell(1).setCellValue("João Silva");
            row.createCell(2).setCellValue("12345678901");
            row.createCell(8).setCellValue("5612");
            row.createCell(9).setCellValue("Vale");
            row.createCell(13).setCellValue(100.0);
        });
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false).processadas());
    }

    @Test
    void importar_valorNumericoCell() throws IOException {
        MockMultipartFile arquivo = workbookComLinhas(
            linha("12345678901", "João Silva", "Vale", "5612", 123.45));
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM)).thenReturn(false);
        when(funcionarioConsultaPort.findByCpfAndAtivoTrue("12345678901")).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findByCodigoAndAtivoTrue("5612")).thenReturn(Optional.of(tipoBeneficio));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(new BigDecimal("123.45"), importacaoBeneficioMensalService.importar(
            arquivo, COMPETENCIA_INICIO, COMPETENCIA_FIM, false).totalValor());
    }

    private MockMultipartFile workbookComLinhaManual(Consumer<Row>... rowFillers) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            sheet.createRow(0);
            for (int i = 0; i < rowFillers.length; i++) {
                var row = sheet.createRow(i + 1);
                rowFillers[i].accept(row);
            }
            return toMultipart(workbook);
        }
    }

    private MockMultipartFile workbookComLinhas(String[]... linhas) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Planilha1");
            var header = sheet.createRow(0);
            String[] headers = {
                    "Matrícula", "Nome", "CPF", "Data Admissão", "Data Rescisão",
                    "Estabelecimento", "C.R.", "Cód. Centro Custo Contabilidade",
                    "Código", "Descrição", "Clas.", "Processo",
                    "MAI/26 - Hora", "MAI/26 - Valor", "MAI/26 - Dt Pgto"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < linhas.length; i++) {
                var row = sheet.createRow(i + 1);
                preencherLinha(row, linhas[i][0], linhas[i][1], linhas[i][2], linhas[i][3],
                        Double.parseDouble(linhas[i][4]));
            }

            return toMultipart(workbook);
        }
    }

    private void preencherLinha(Row row, String cpf, String nome, String descricao, String codigo, double valor) {
        row.createCell(1).setCellValue(nome);
        row.createCell(2).setCellValue(Long.parseLong(cpf));
        row.createCell(8).setCellValue(Integer.parseInt(codigo));
        row.createCell(9).setCellValue(descricao);
        row.createCell(13).setCellValue(valor);
    }

    private String[] linha(String cpf, String nome, String descricao, String codigo, double valor) {
        return new String[]{cpf, nome, descricao, codigo, String.valueOf(valor)};
    }

    private MockMultipartFile toMultipart(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return new MockMultipartFile(
                "file",
                "beneficios.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );
    }
}
