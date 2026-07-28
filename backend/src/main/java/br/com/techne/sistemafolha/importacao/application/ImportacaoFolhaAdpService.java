package br.com.techne.sistemafolha.importacao.application;

import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioImportRef;
import br.com.techne.sistemafolha.cadastros.port.RubricaImportRef;
import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaDuplicadaException;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoLinhaCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoPort;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoResumoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaProcessamentoPort;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportacaoFolhaAdpService {

    private static final Logger logger = LoggerFactory.getLogger(ImportacaoFolhaAdpService.class);
    private static final String DOMAIN = "importacao";

    private final CadastrosImportLookupPort cadastrosImportLookupPort;
    private final FolhaConsultaPort folhaConsultaPort;
    private final FolhaImportacaoPort folhaImportacaoPort;
    private final FolhaProcessamentoPort folhaProcessamentoPort;

    private final List<String> rubricasIgnore = List.of(
        "VENDAS E PRE VENDAS-CRONA"
    );

    private final Map<String, String> empresa = new HashMap<>();

    private static final Pattern TOTAL_EMPREGADOS_PATTERN = Pattern.compile("Total de Empregados\\s*:\\s*(\\d+)");
    private static final Pattern TOTAL_ENCARGOS_PATTERN = Pattern.compile("Total de Encargos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_PAGAMENTOS_PATTERN = Pattern.compile("Total de Pagamentos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_DESCONTOS_PATTERN = Pattern.compile("Total de Descontos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_LIQUIDO_PATTERN = Pattern.compile("Total Líquido\\s*:\\s*([\\d.,]+)");

    public ImportacaoFolhaAdpService(
            CadastrosImportLookupPort cadastrosImportLookupPort,
            FolhaConsultaPort folhaConsultaPort,
            FolhaImportacaoPort folhaImportacaoPort,
            FolhaProcessamentoPort folhaProcessamentoPort) {
        this.cadastrosImportLookupPort = cadastrosImportLookupPort;
        this.folhaConsultaPort = folhaConsultaPort;
        this.folhaImportacaoPort = folhaImportacaoPort;
        this.folhaProcessamentoPort = folhaProcessamentoPort;
        inicializarMapaEmpresas();
    }

    private void inicializarMapaEmpresas() {
        empresa.put("258", "Filial    0065  TECHNE - EDUCACAO");
        empresa.put("149", "Filial    0065  TECHNE - EDUCACAO");
        empresa.put("245", "Filial    0065  TECHNE - EDUCACAO");
    }

    @Transactional
    public ImportacaoFolhaAdpResult importarFolhaAdp(
            MultipartFile arquivo, Boolean decimoTerceiro, Boolean confirmarSubstituicao) throws IOException {
        logger.info("{}Iniciando importação de folha ADP - Arquivo: {}, Tamanho: {} bytes",
                   DomainLogging.prefix(DOMAIN), arquivo.getOriginalFilename(), arquivo.getSize());

        LocalDate[] periodo = extrairPeriodoCompetencia(arquivo);
        LocalDate dataInicio = periodo[0];
        LocalDate dataFim = periodo[1];

        logger.info("Período de competência identificado: {} a {}", dataInicio, dataFim);

        boolean isDecimoTerceiro = decimoTerceiro != null ? decimoTerceiro : (dataInicio.getMonthValue() == 12);
        boolean substituirExistente = false;

        if (folhaConsultaPort.existsResumoAtivo(dataInicio, dataFim, isDecimoTerceiro)) {
            if (!Boolean.TRUE.equals(confirmarSubstituicao)) {
                String tipoFolha = isDecimoTerceiro ? "13º salário" : "normal";
                throw new FolhaDuplicadaException(
                    "Já existe uma folha de pagamento " + tipoFolha +
                    " para o período " + dataInicio + " a " + dataFim +
                    ". A importação desta nova folha irá substituir a folha existente. Deseja continuar?",
                    dataInicio.toString(),
                    dataFim.toString(),
                    isDecimoTerceiro
                );
            }
            substituirExistente = true;
            logger.info("Substituição confirmada para o período {} a {}", dataInicio, dataFim);
        }

        List<FolhaImportacaoLinhaCommand> linhas = new ArrayList<>();
        FuncionarioImportRef funcionarioAtual = null;
        List<String> funcionariosNaoEncontrados = new ArrayList<>();
        Set<String> conflitosCpfCompetencia = new LinkedHashSet<>();

        Integer totalEmpregados = null;
        BigDecimal totalEncargos = null;
        BigDecimal totalPagamentos = null;
        BigDecimal totalDescontos = null;
        BigDecimal totalLiquido = null;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), Charset.forName("WINDOWS-1252")))) {

            while (br.ready()) {
                String linha = br.readLine();

                if (linha.length() > 102 && linha.substring(96, 102).equalsIgnoreCase("Admiss")) {
                    String identificadorFuncionario = linha.substring(57, 96).trim();
                    String idExterno = linha.substring(50, 55).trim();

                    funcionarioAtual = cadastrosImportLookupPort.findFuncionarioByIdExterno(idExterno)
                            .orElse(null);

                    if (funcionarioAtual == null) {
                        logger.warn("Funcionário não encontrado: {}", identificadorFuncionario);
                        funcionariosNaoEncontrados.add(identificadorFuncionario);
                    } else {
                        logger.debug("Funcionário encontrado: {} - {}", funcionarioAtual.idExterno(), funcionarioAtual.nome());
                    }
                }

                if (linha.length() > 6 && !linha.substring(0, 4).isBlank() &&
                    linha.substring(4, 5).isBlank() && !linha.substring(5, 6).isBlank()) {

                    if (linha.substring(0, 4).equalsIgnoreCase("¯¯¯¯") ||
                        linha.substring(0, 3).equalsIgnoreCase("Evt")) {
                        continue;
                    }

                    if (linha.length() == 130) {
                        linha += " ";
                    }

                    processarRubrica(linha, 0, 31, 32, 65, funcionarioAtual, dataInicio, dataFim,
                        linhas, conflitosCpfCompetencia, substituirExistente, isDecimoTerceiro);

                    if (linha.length() > 130 &&
                        !linha.substring(66, 97).trim().isEmpty() &&
                        !linha.substring(66, 70).isBlank()) {

                        String segundaRubrica = linha.substring(66, 97).trim();
                        String segundaValores = linha.substring(98, 131).trim();

                        if (!segundaRubrica.isEmpty() && !segundaValores.isEmpty()) {
                            processarRubrica(linha, 66, 97, 98, 131, funcionarioAtual, dataInicio, dataFim,
                                linhas, conflitosCpfCompetencia, substituirExistente, isDecimoTerceiro);
                        } else {
                            logger.debug("Segunda rubrica vazia ou sem valores válidos, pulando");
                        }
                    } else {
                        logger.debug("Linha não tem segunda rubrica ou comprimento insuficiente");
                    }
                }

                Matcher totalEmpregadosMatcher = TOTAL_EMPREGADOS_PATTERN.matcher(linha);
                if (totalEmpregadosMatcher.find()) {
                    try {
                        totalEmpregados = Integer.parseInt(totalEmpregadosMatcher.group(1));
                        logger.info("Total de Empregados identificado: {}", totalEmpregados);
                    } catch (Exception e) {
                        logger.error("Erro ao processar total de empregados na linha: {}", linha);
                    }
                }

                Matcher totalEncargosMatcher = TOTAL_ENCARGOS_PATTERN.matcher(linha);
                if (totalEncargosMatcher.find()) {
                    try {
                        totalEncargos = parseBigDecimal(totalEncargosMatcher.group(1));
                        logger.info("Total de Encargos identificado: {}", totalEncargos);
                    } catch (Exception e) {
                        logger.error("Erro ao processar total de encargos na linha: {}", linha);
                    }
                }

                Matcher totalPagamentosMatcher = TOTAL_PAGAMENTOS_PATTERN.matcher(linha);
                if (totalPagamentosMatcher.find()) {
                    try {
                        totalPagamentos = parseBigDecimal(totalPagamentosMatcher.group(1));
                        logger.info("Total de Pagamentos identificado: {}", totalPagamentos);
                    } catch (Exception e) {
                        logger.error("Erro ao processar total de pagamentos na linha: {}", linha);
                    }
                }

                Matcher totalDescontosMatcher = TOTAL_DESCONTOS_PATTERN.matcher(linha);
                if (totalDescontosMatcher.find()) {
                    try {
                        totalDescontos = parseBigDecimal(totalDescontosMatcher.group(1));
                        logger.info("Total de Descontos identificado: {}", totalDescontos);
                    } catch (Exception e) {
                        logger.error("Erro ao processar total de descontos na linha: {}", linha);
                    }
                }

                Matcher totalLiquidoMatcher = TOTAL_LIQUIDO_PATTERN.matcher(linha);
                if (totalLiquidoMatcher.find()) {
                    try {
                        totalLiquido = parseBigDecimal(totalLiquidoMatcher.group(1));
                        logger.info("Total Líquido identificado: {}", totalLiquido);
                    } catch (Exception e) {
                        logger.error("Erro ao processar total líquido na linha: {}", linha);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro durante importação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro durante importação: " + e.getMessage(), e);
        }

        if (!conflitosCpfCompetencia.isEmpty()) {
            throw new RuntimeException(
                "Folha duplicada por CPF na mesma competência: " + String.join("; ", conflitosCpfCompetencia));
        }

        if (!funcionariosNaoEncontrados.isEmpty()) {
            throw new RuntimeException("Funcionários não encontrados: " + String.join(", ", funcionariosNaoEncontrados));
        }

        FolhaImportacaoResumoCommand resumo = null;
        if (totalEmpregados != null && totalEncargos != null && totalPagamentos != null
            && totalDescontos != null && totalLiquido != null) {
            resumo = new FolhaImportacaoResumoCommand(
                totalEmpregados, totalEncargos, totalPagamentos, totalDescontos, totalLiquido);
        } else {
            logger.warn("Dados de resumo incompletos - não foi possível salvar o resumo");
        }

        FolhaImportacaoCommand command = new FolhaImportacaoCommand(
            dataInicio,
            dataFim,
            isDecimoTerceiro,
            substituirExistente,
            linhas,
            resumo
        );

        List<FolhaPagamentoDTO> persistidas = folhaImportacaoPort.persistirImportacao(command);
        logger.info("Importação de folha ADP concluída - Registros processados: {}", persistidas.size());

        ProcessamentoResultadoDTO processamento = folhaProcessamentoPort.processar(
            dataInicio, dataFim, isDecimoTerceiro, false);
        logger.info("Processamento de ficha concluído - Fichas: {}, Linhas: {}",
            processamento.totalFichas(), processamento.totalLinhas());

        return new ImportacaoFolhaAdpResult(persistidas, processamento);
    }

    private void processarRubrica(
            String linha, int inicioRubrica, int fimRubrica, int inicioValores, int fimValores,
            FuncionarioImportRef funcionario, LocalDate dataInicio, LocalDate dataFim,
            List<FolhaImportacaoLinhaCommand> linhas, Set<String> conflitosCpfCompetencia,
            boolean substituirExistente, boolean decimoTerceiro) {

        if (funcionario == null) {
            logger.warn("Funcionário não encontrado, pulando rubrica");
            return;
        }

        try {
            if (linha.length() < fimValores) {
                logger.warn("Linha muito curta para processar rubrica: comprimento={}, necessário={}",
                           linha.length(), fimValores);
                return;
            }

            String rubricaCompleta = linha.substring(inicioRubrica, fimRubrica).trim();
            String valoresCompletos = linha.substring(inicioValores, fimValores).trim();

            if (rubricaCompleta.isEmpty() || valoresCompletos.isEmpty()) {
                logger.debug("Rubrica ou valores vazios, pulando: rubrica='{}', valores='{}'",
                           rubricaCompleta, valoresCompletos);
                return;
            }

            logger.debug("Processando rubrica - Linha: '{}', Rubrica: '{}', Valores: '{}'",
                       linha, rubricaCompleta, valoresCompletos);

            String[] partesRubrica = rubricaCompleta.split("\\s+", 2);
            if (partesRubrica.length < 2) {
                logger.warn("Formato de rubrica inválido: '{}'", rubricaCompleta);
                return;
            }

            String codigoRubrica = partesRubrica[0].trim();
            String descricaoRubrica = partesRubrica[1].trim();

            Pattern pattern = Pattern.compile("([\\d.,]+)\\s+([\\d.,]+)\\s+([\\d.,]+[+-]?)");
            Matcher matcher = pattern.matcher(valoresCompletos);

            if (!matcher.find()) {
                logger.error("Não foi possível extrair valores da string: '{}'", valoresCompletos);
                return;
            }

            String quantidade = matcher.group(1);
            String baseCalculo = matcher.group(2);
            String valor = matcher.group(3);

            BigDecimal quantidadeBD = parseBigDecimal(quantidade);
            BigDecimal baseCalculoBD = parseBigDecimal(baseCalculo);
            BigDecimal valorBD = parseBigDecimal(valor);

            String tipo = valor.endsWith("+") ? "+" : valor.endsWith("-") ? "-" : "";
            String tipoRubricaDescricao = determinarTipoRubricaDescricao(tipo);

            logger.debug("Processando rubrica: Código={}, Descrição={}, Valor={}, Funcionário={}",
                       codigoRubrica, descricaoRubrica, valorBD, funcionario.nome());

            RubricaImportRef rubrica = cadastrosImportLookupPort.findOrCreateRubrica(
                codigoRubrica, descricaoRubrica, tipoRubricaDescricao);

            if (folhaConsultaPort.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
                    funcionario.cpf(), funcionario.id(), dataInicio, dataFim, decimoTerceiro)) {
                String msg = String.format(
                    "CPF %s já possui folha ativa no período %s a %s (funcionário atual: %s, matrícula %s)",
                    funcionario.cpf(), dataInicio, dataFim, funcionario.nome(), funcionario.idExterno());
                conflitosCpfCompetencia.add(msg);
                logger.warn(msg);
                return;
            }

            // Em substituição, as linhas antigas serão apagadas — não pular por existência prévia.
            boolean jaExiste = !substituirExistente
                && folhaConsultaPort.existsByFuncionarioIdAndRubricaIdAndPeriodo(
                    funcionario.id(), rubrica.id(), dataInicio, dataFim, decimoTerceiro);

            if (!jaExiste) {
                linhas.add(new FolhaImportacaoLinhaCommand(
                    funcionario.id(),
                    rubrica.id(),
                    funcionario.cargoId(),
                    funcionario.centroCustoId(),
                    funcionario.linhaNegocioId(),
                    valorBD,
                    quantidadeBD,
                    baseCalculoBD
                ));

                logger.debug("Linha de importação criada: Funcionário={}, Rubrica={}, Valor={}",
                           funcionario.nome(), rubrica.codigo(), valorBD);
            } else {
                logger.debug("Registro já existe para funcionário={}, rubrica={}, período={}",
                           funcionario.nome(), rubrica.codigo(), dataInicio);
            }
        } catch (StringIndexOutOfBoundsException e) {
            logger.error("Erro de índice ao processar rubrica: {} - Linha: '{}' (comprimento: {})",
                       e.getMessage(), linha, linha.length());
        } catch (Exception e) {
            logger.error("Erro ao processar rubrica: {} - Linha: '{}'", e.getMessage(), linha);
        }
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String valorLimpo = valor.trim()
            .replace("+", "")
            .replace("-", "")
            .replace(".", "")
            .replace(",", ".")
            .trim();

        if (valorLimpo.isEmpty() || valorLimpo.equals(".")) {
            logger.warn("Valor vazio ou inválido após limpeza: '{}'", valor);
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(valorLimpo);
        } catch (NumberFormatException e) {
            logger.error("Erro ao converter valor: '{}' -> '{}' - Erro: {}", valor, valorLimpo, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private String determinarTipoRubricaDescricao(String tipo) {
        if (tipo == null || tipo.isEmpty()) {
            return "INFORMATIVO";
        }
        if ("+".equals(tipo)) {
            return "PROVENTO";
        }
        if ("-".equals(tipo)) {
            return "DESCONTO";
        }
        return "INFORMATIVO";
    }

    private LocalDate[] extrairPeriodoCompetencia(MultipartFile arquivo) throws IOException {
        LocalDate dataInicio = LocalDate.now();
        LocalDate dataFim = LocalDate.now();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), Charset.forName("WINDOWS-1252")))) {

            while (br.ready()) {
                String linha = br.readLine();

                if (linha.contains("Competência:")) {
                    try {
                        String[] partes = linha.split("Competência:\\s*");
                        if (partes.length > 1) {
                            String periodoStr = partes[1].trim();
                            String[] datas = periodoStr.split("\\s+a\\s+");
                            if (datas.length == 2) {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                dataInicio = LocalDate.parse(datas[0].trim(), formatter);
                                dataFim = LocalDate.parse(datas[1].trim(), formatter);
                                logger.info("Período extraído do arquivo: {} a {}", dataInicio, dataFim);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Erro ao extrair período de competência: {}", e.getMessage());
                    }
                }
            }
        }

        return new LocalDate[]{dataInicio, dataFim};
    }
}
