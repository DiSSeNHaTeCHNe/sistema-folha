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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ImportacaoBeneficioMensalService {

    private static final Logger logger = LoggerFactory.getLogger(ImportacaoBeneficioMensalService.class);
    private static final String DOMAIN = "beneficios";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);
    private static final String ABA_PLANILHA = "Planilha1";
    private static final int COL_CPF = 2;
    private static final int COL_NOME = 1;
    private static final int COL_DESCRICAO = 9;
    private static final int COL_CODIGO = 8;
    private static final int COL_VALOR = 13;
    private static final int PRIMEIRA_LINHA_DADOS = 1;

    private final FuncionarioConsultaPort funcionarioConsultaPort;
    private final TipoBeneficioRepository tipoBeneficioRepository;
    private final BeneficioMensalRepository beneficioMensalRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public ImportacaoBeneficioMensalService(
            FuncionarioConsultaPort funcionarioConsultaPort,
            TipoBeneficioRepository tipoBeneficioRepository,
            BeneficioMensalRepository beneficioMensalRepository) {
        this.funcionarioConsultaPort = funcionarioConsultaPort;
        this.tipoBeneficioRepository = tipoBeneficioRepository;
        this.beneficioMensalRepository = beneficioMensalRepository;
    }

    @Transactional
    public ImportacaoResultadoDTO importar(
            MultipartFile arquivo,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            Boolean confirmar) throws IOException {

        validarParametrosImportacao(arquivo, competenciaInicio, competenciaFim);

        boolean confirmarSubstituicao = Boolean.TRUE.equals(confirmar);

        logger.info("{}Iniciando importação de benefícios mensais - Arquivo: {}, competência: {} a {}",
                DOMAIN_PREFIX, arquivo.getOriginalFilename(), competenciaInicio, competenciaFim);

        verificarDuplicidadeSemConfirmacao(competenciaInicio, competenciaFim, confirmarSubstituicao);

        ResultadoProcessamentoPlanilha resultado;
        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = obterAbaPlanilha(workbook);
            resultado = processarPlanilha(sheet, competenciaInicio, competenciaFim);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Erro ao processar arquivo xlsx: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Arquivo inválido ou corrompido: " + ex.getMessage());
        }

        if (!resultado.detalhesErros().isEmpty()) {
            logger.warn("Importação rejeitada - {} erro(s) encontrado(s), nenhum registro persistido",
                    resultado.detalhesErros().size());
            throw new ImportacaoBeneficioMensalInvalidaException(resultado.detalhesErros());
        }

        persistirRegistros(resultado.registros(), competenciaInicio, competenciaFim, confirmarSubstituicao);

        logger.info("{}Importação concluída - processadas: {}, total: {}", DOMAIN_PREFIX,
                resultado.registros().size(), resultado.totalValor());

        return new ImportacaoResultadoDTO(
                resultado.registros().size(),
                resultado.detalhesErros().size(),
                resultado.totalValor(),
                resultado.detalhesErros()
        );
    }

    private void validarParametrosImportacao(
            MultipartFile arquivo,
            LocalDate competenciaInicio,
            LocalDate competenciaFim) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de importação é obrigatório");
        }
        if (competenciaInicio == null || competenciaFim == null) {
            throw new IllegalArgumentException("Competência de início e fim são obrigatórias");
        }
    }

    private Sheet obterAbaPlanilha(Workbook workbook) {
        Sheet sheet = workbook.getSheet(ABA_PLANILHA);
        if (sheet == null) {
            throw new IllegalArgumentException("Aba '" + ABA_PLANILHA + "' não encontrada no arquivo");
        }
        return sheet;
    }

    private record ResultadoProcessamentoPlanilha(
            List<BeneficioMensal> registros,
            List<String> detalhesErros,
            BigDecimal totalValor) {
    }

    private ResultadoProcessamentoPlanilha processarPlanilha(
            Sheet sheet,
            LocalDate competenciaInicio,
            LocalDate competenciaFim) {

        List<String> detalhesErros = new ArrayList<>();
        List<BeneficioMensal> registros = new ArrayList<>();
        BigDecimal totalValor = BigDecimal.ZERO;

        for (int rowIndex = PRIMEIRA_LINHA_DADOS; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Optional<BeneficioMensal> beneficioOpt = processarLinha(
                    row, rowIndex, competenciaInicio, competenciaFim, detalhesErros);
            if (beneficioOpt.isPresent()) {
                BeneficioMensal beneficio = beneficioOpt.get();
                registros.add(beneficio);
                totalValor = totalValor.add(beneficio.getValor());
            }
        }

        return new ResultadoProcessamentoPlanilha(registros, detalhesErros, totalValor);
    }

    private Optional<BeneficioMensal> processarLinha(
            Row row,
            int rowIndex,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            List<String> detalhesErros) {

        String cpf = normalizarCpf(lerTexto(row.getCell(COL_CPF)));
        if (cpf.isEmpty()) {
            return Optional.empty();
        }

        int linhaPlanilha = rowIndex + 1;
        String nome = lerTexto(row.getCell(COL_NOME));
        String descricao = lerTexto(row.getCell(COL_DESCRICAO));
        String codigo = lerCodigoTipoBeneficio(row.getCell(COL_CODIGO));

        Funcionario funcionario = funcionarioConsultaPort.findByCpfAndAtivoTrue(cpf).orElse(null);
        if (funcionario == null) {
            detalhesErros.add(formatarErro(linhaPlanilha, cpf, nome,
                    "Funcionário ativo não encontrado para o CPF informado"));
            return Optional.empty();
        }

        if (codigo.isEmpty()) {
            detalhesErros.add(formatarErro(linhaPlanilha, cpf, nome,
                    "Código do tipo de benefício é obrigatório"));
            return Optional.empty();
        }

        TipoBeneficio tipoBeneficio = tipoBeneficioRepository.findByCodigoAndAtivoTrue(codigo).orElse(null);
        if (tipoBeneficio == null) {
            detalhesErros.add(formatarErro(linhaPlanilha, cpf, nome,
                    "Tipo de benefício não encontrado para o código: " + codigo));
            return Optional.empty();
        }

        BigDecimal valor;
        try {
            valor = lerValor(row.getCell(COL_VALOR));
        } catch (IllegalArgumentException ex) {
            detalhesErros.add(formatarErro(linhaPlanilha, cpf, nome, ex.getMessage()));
            return Optional.empty();
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            detalhesErros.add(formatarErro(linhaPlanilha, cpf, nome,
                    "Valor deve ser maior ou igual a zero"));
            return Optional.empty();
        }

        BeneficioMensal beneficio = criarBeneficioMensal(
                funcionario, tipoBeneficio, valor, competenciaInicio, competenciaFim, descricao);
        return Optional.of(beneficio);
    }

    private BeneficioMensal criarBeneficioMensal(
            Funcionario funcionario,
            TipoBeneficio tipoBeneficio,
            BigDecimal valor,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            String descricao) {

        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setFuncionario(funcionario);
        beneficio.setTipoBeneficio(tipoBeneficio);
        beneficio.setValor(valor);
        beneficio.setCompetenciaInicio(competenciaInicio);
        beneficio.setCompetenciaFim(competenciaFim);
        if (!descricao.isEmpty()) {
            beneficio.setObservacao(descricao);
        }
        beneficio.setAtivo(true);
        return beneficio;
    }

    private void persistirRegistros(
            List<BeneficioMensal> registros,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean confirmarSubstituicao) {

        if (confirmarSubstituicao) {
            substituirRegistrosExistentes(competenciaInicio, competenciaFim);
        }

        for (BeneficioMensal registro : registros) {
            beneficioMensalRepository.save(registro);
        }
    }

    private void verificarDuplicidadeSemConfirmacao(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean confirmarSubstituicao) {

        if (!beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                competenciaInicio, competenciaFim)) {
            return;
        }

        if (!confirmarSubstituicao) {
            throw new BeneficioMensalDuplicadaException(
                    "Já existem lançamentos de benefícios mensais para o período "
                            + competenciaInicio + " a " + competenciaFim
                            + ". A importação irá substituir os registros existentes. Deseja continuar?",
                    competenciaInicio.toString(),
                    competenciaFim.toString()
            );
        }
    }

    private void substituirRegistrosExistentes(LocalDate competenciaInicio, LocalDate competenciaFim) {
        logger.info("{}Removendo registros existentes (ativos e inativos) para competência {} a {}", DOMAIN_PREFIX,                 competenciaInicio, competenciaFim);
        beneficioMensalRepository.deleteByCompetenciaInicioAndCompetenciaFim(competenciaInicio, competenciaFim);
    }

    private String lerTexto(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private String lerCodigoTipoBeneficio(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return lerTexto(cell);
    }

    private BigDecimal lerValor(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("Valor é obrigatório");
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        String texto = lerTexto(cell);
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Valor é obrigatório");
        }

        String normalizado = texto
                .replace("R$", "")
                .replace(" ", "")
                .trim();

        if (normalizado.contains(",")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        }

        try {
            return new BigDecimal(normalizado).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido: " + texto);
        }
    }

    private String normalizarCpf(String cpfBruto) {
        if (cpfBruto == null || cpfBruto.isBlank()) {
            return "";
        }
        String apenasDigitos = cpfBruto.replaceAll("\\D", "");
        if (apenasDigitos.isEmpty()) {
            return "";
        }
        if (apenasDigitos.length() < 11) {
            apenasDigitos = String.format("%011d", Long.parseLong(apenasDigitos));
        }
        return apenasDigitos;
    }

    private String formatarErro(int linha, String cpf, String nome, String mensagem) {
        String identificacao = nome.isEmpty() ? cpf : cpf + " - " + nome;
        return "Linha " + linha + " (" + identificacao + "): " + mensagem;
    }
}
