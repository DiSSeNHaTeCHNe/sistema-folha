package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.model.TipoRubrica;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.RubricaRepository;
import br.com.techne.sistemafolha.repository.TipoRubricaRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ImportacaoFolhaAdpService {

    private static final Logger logger = LoggerFactory.getLogger(ImportacaoFolhaAdpService.class);

    private final FuncionarioRepository funcionarioRepository;
    private final RubricaRepository rubricaRepository;
    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final TipoRubricaRepository tipoRubricaRepository;
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    // Lista de rubricas para ignorar (substituir por "--")
    private final List<String> rubricasIgnore = List.of(
        "VENDAS E PRE VENDAS-CRONA"
    );

    // Mapa de empresas/centros de custo
    private final Map<String, String> empresa = new HashMap<>();
    
    // Padrões para extrair dados de resumo
    private static final Pattern TOTAL_EMPREGADOS_PATTERN = Pattern.compile("Total de Empregados\\s*:\\s*(\\d+)");
    private static final Pattern TOTAL_ENCARGOS_PATTERN = Pattern.compile("Total de Encargos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_PAGAMENTOS_PATTERN = Pattern.compile("Total de Pagamentos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_DESCONTOS_PATTERN = Pattern.compile("Total de Descontos\\s*:\\s*([\\d.,]+)");
    private static final Pattern TOTAL_LIQUIDO_PATTERN = Pattern.compile("Total Líquido\\s*:\\s*([\\d.,]+)");

    public ImportacaoFolhaAdpService(
            FuncionarioRepository funcionarioRepository,
            RubricaRepository rubricaRepository,
            FolhaPagamentoRepository folhaPagamentoRepository,
            TipoRubricaRepository tipoRubricaRepository,
            ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.rubricaRepository = rubricaRepository;
        this.folhaPagamentoRepository = folhaPagamentoRepository;
        this.tipoRubricaRepository = tipoRubricaRepository;
        this.resumoFolhaPagamentoRepository = resumoFolhaPagamentoRepository;
        
        // Inicializar mapa de empresas
        inicializarMapaEmpresas();
    }

    private void inicializarMapaEmpresas() {
        empresa.put("258", "Filial    0065  TECHNE - EDUCACAO");
        empresa.put("149", "Filial    0065  TECHNE - EDUCACAO");
        empresa.put("245", "Filial    0065  TECHNE - EDUCACAO");
        // Adicione mais mapeamentos conforme necessário
    }

    @Transactional
    public List<FolhaPagamento> importarFolhaAdp(MultipartFile arquivo) throws IOException {
        logger.info("Iniciando importação de folha ADP - Arquivo: {}, Tamanho: {} bytes", 
                   arquivo.getOriginalFilename(), arquivo.getSize());

        List<FolhaPagamento> folhasPagamento = new ArrayList<>();
        LocalDate dataInicio = LocalDate.now(); // Será extraída do arquivo se disponível
        LocalDate dataFim = LocalDate.now(); // Será extraída do arquivo se disponível
        
        Funcionario funcionarioAtual = null;
        List<String> rubricasProcessadas = new ArrayList<>();
        
        // Variáveis para dados de resumo
        Integer totalEmpregados = null;
        BigDecimal totalEncargos = null;
        BigDecimal totalPagamentos = null;
        BigDecimal totalDescontos = null;
        BigDecimal totalLiquido = null;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), Charset.forName("WINDOWS-1252")))) {

            while (br.ready()) {
                String linha = br.readLine();

                // Procura período de competência
                if (linha.contains("Competência:")) {
                    try {
                        // Extrai período de competência se disponível
                        // Formato esperado: "Competência: 01/10/2023 a 31/10/2023"
                        String[] partes = linha.split("Competência:\\s*");
                        if (partes.length > 1) {
                            String periodo = partes[1].trim();
                            String[] datas = periodo.split("\\s+a\\s+");
                            if (datas.length == 2) {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                dataInicio = LocalDate.parse(datas[0].trim(), formatter);
                                dataFim = LocalDate.parse(datas[1].trim(), formatter);
                                logger.info("Período de competência identificado: {} a {}", dataInicio, dataFim);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Erro ao processar período de competência: {}", e.getMessage());
                    }
                }

                // Processa cabeçalho do funcionário (Admissão)
                if (linha.length() > 102 && linha.substring(96, 102).equalsIgnoreCase("Admiss")) {
                    String identificadorFuncionario = linha.substring(57, 96).trim();
                    String idExterno = linha.substring(50, 55).trim();
                    
                    // Busca funcionário por identificador
                    funcionarioAtual = funcionarioRepository.findByIdExterno(idExterno)
                            .orElse(null);

                    if (funcionarioAtual == null) {
                        logger.warn("Funcionário não encontrado: {}", identificadorFuncionario);
                    } else {
                        logger.debug("Funcionário encontrado: {} - {}", funcionarioAtual.getIdExterno(), funcionarioAtual.getNome());
                    }
                }

                // Processa linhas de rubricas (posições fixas)
                if (linha.length() > 6 && !linha.substring(0, 4).isBlank() && 
                    linha.substring(4, 5).isBlank() && !linha.substring(5, 6).isBlank()) {
                    
                    // Ignora linhas especiais
                    if (linha.substring(0, 4).equalsIgnoreCase("¯¯¯¯") ||
                        linha.substring(0, 3).equalsIgnoreCase("Evt")) {
                        continue;
                    }

                    if(linha.length() == 130)
                        linha += " ";

                    // Processa primeira rubrica da linha
                    // Formato: "0010 Salário Base           200,00         0,00        13.250,54+"
                    processarRubrica(linha, 0, 31, 32, 65, funcionarioAtual, dataInicio, dataFim, folhasPagamento);
                    
                    // Processa segunda rubrica da linha (se existir)
                    // Verifica se a linha tem comprimento suficiente e se há conteúdo na segunda rubrica
                    if (linha.length() > 130 &&
                        !linha.substring(66, 97).trim().isEmpty() && 
                        !linha.substring(66, 70).isBlank()) {

                        // Verifica se há valores válidos para a segunda rubrica
                        String segundaRubrica = linha.substring(66, 97).trim();
                        String segundaValores = linha.substring(98, 131).trim();
                        
                        // Só processa se há conteúdo válido na segunda rubrica
                        if (!segundaRubrica.isEmpty() && !segundaValores.isEmpty()) {
                            processarRubrica(linha, 66, 97, 98, 131, funcionarioAtual, dataInicio, dataFim, folhasPagamento);
                        } else {
                            logger.debug("Segunda rubrica vazia ou sem valores válidos, pulando");
                        }
                    } else {
                        logger.debug("Linha não tem segunda rubrica ou comprimento insuficiente");
                    }
                }
                
                // Processa dados de resumo no final do arquivo
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

            // Salva todas as folhas de pagamento no banco
            for (FolhaPagamento folha : folhasPagamento) {
                folhaPagamentoRepository.save(folha);
            }
            
            // Salva o resumo se todos os dados foram encontrados
            if (dataInicio != null && dataFim != null && totalEmpregados != null && 
                totalEncargos != null && totalPagamentos != null && totalDescontos != null && totalLiquido != null) {
                try {
                    ResumoFolhaPagamento resumo = new ResumoFolhaPagamento();
                    resumo.setTotalEmpregados(totalEmpregados);
                    resumo.setTotalEncargos(totalEncargos);
                    resumo.setTotalPagamentos(totalPagamentos);
                    resumo.setTotalDescontos(totalDescontos);
                    resumo.setTotalLiquido(totalLiquido);
                    resumo.setCompetenciaInicio(dataInicio);
                    resumo.setCompetenciaFim(dataFim);
                    resumo.setDataImportacao(LocalDateTime.now());
                    resumo.setAtivo(true);
                    
                    resumoFolhaPagamentoRepository.save(resumo);
                    logger.info("Resumo da folha de pagamento ADP salvo com sucesso");
                } catch (Exception e) {
                    logger.error("Erro ao salvar resumo da folha de pagamento ADP: {}", e.getMessage());
                }
            } else {
                logger.warn("Dados de resumo incompletos - não foi possível salvar o resumo");
            }

        } catch (Exception e) {
            logger.error("Erro durante importação: {}", e.getMessage(), e);
            throw new RuntimeException("Erro durante importação: " + e.getMessage(), e);
        }

        logger.info("Importação de folha ADP concluída - Registros processados: {}", folhasPagamento.size());
        return folhasPagamento;
    }

    private void processarRubrica(String linha, int inicioRubrica, int fimRubrica, int inicioValores, int fimValores, 
                                 Funcionario funcionario, LocalDate dataInicio, LocalDate dataFim, 
                                 List<FolhaPagamento> folhasPagamento) {
        
        if (funcionario == null) {
            logger.warn("Funcionário não encontrado, pulando rubrica");
            return;
        }

        try {
            // Verifica se a linha tem comprimento suficiente para extrair os dados
            if (linha.length() < fimValores) {
                logger.warn("Linha muito curta para processar rubrica: comprimento={}, necessário={}", 
                           linha.length(), fimValores);
                return;
            }

            // Extrai dados da rubrica
            String rubricaCompleta = linha.substring(inicioRubrica, fimRubrica).trim();
            String valoresCompletos = linha.substring(inicioValores, fimValores).trim();
            
            // Verifica se os dados extraídos são válidos
            if (rubricaCompleta.isEmpty() || valoresCompletos.isEmpty()) {
                logger.debug("Rubrica ou valores vazios, pulando: rubrica='{}', valores='{}'", 
                           rubricaCompleta, valoresCompletos);
                return;
            }
            
            // Log para debug
            logger.debug("Processando rubrica - Linha: '{}', Rubrica: '{}', Valores: '{}'", 
                       linha, rubricaCompleta, valoresCompletos);
            
            // Parse da rubrica (formato: "0010 Salário Base")
            String[] partesRubrica = rubricaCompleta.split("\\s+", 2);
            if (partesRubrica.length < 2) {
                logger.warn("Formato de rubrica inválido: '{}'", rubricaCompleta);
                return;
            }
            
            String codigoRubrica = partesRubrica[0].trim();
            String descricaoRubrica = partesRubrica[1].trim();
            
            // Parse dos valores usando regex para extrair os três valores
            // Formato esperado: "200,00         0,00        13.250,54+"
            Pattern pattern = Pattern.compile("([\\d.,]+)\\s+([\\d.,]+)\\s+([\\d.,]+[+-]?)");
            Matcher matcher = pattern.matcher(valoresCompletos);
            
            if (matcher.find()) {
                String quantidade = matcher.group(1);
                String baseCalculo = matcher.group(2);
                String valor = matcher.group(3);
                
                logger.debug("Valores extraídos: Quantidade='{}', Base='{}', Valor='{}'", 
                           quantidade, baseCalculo, valor);
                
                BigDecimal quantidadeBD = parseBigDecimal(quantidade);
                BigDecimal baseCalculoBD = parseBigDecimal(baseCalculo);
                BigDecimal valorBD = parseBigDecimal(valor);
                
                // Determina tipo da rubrica pelo último caractere
                String tipo = valor.endsWith("+") ? "+" : valor.endsWith("-") ? "-" : "";
                
                logger.debug("Processando rubrica: Código={}, Descrição={}, Valor={}, Funcionário={}", 
                           codigoRubrica, descricaoRubrica, valorBD, funcionario.getNome());

                // Busca ou cria rubrica
                Rubrica rubrica = rubricaRepository.findByCodigo(codigoRubrica)
                    .orElseGet(() -> {
                        logger.info("Criando nova rubrica: Código={}, Descrição={}, Tipo={}", 
                                  codigoRubrica, descricaoRubrica, determinarTipoRubrica(tipo));
                        Rubrica novaRubrica = new Rubrica();
                        novaRubrica.setCodigo(codigoRubrica);
                        novaRubrica.setDescricao(descricaoRubrica);
                        novaRubrica.setTipoRubrica(determinarTipoRubrica(tipo));
                        novaRubrica.setPorcentagem(100.0); // Valor padrão de 100%
                        return rubricaRepository.save(novaRubrica);
                    });

                // Verifica se já existe registro para este funcionário, rubrica e período
                if (!folhaPagamentoRepository.existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
                        funcionario.getId(), rubrica.getId(), dataInicio, dataFim)) {
                    
                    // Cria objeto FolhaPagamento
                    FolhaPagamento folha = new FolhaPagamento();
                    folha.setFuncionario(funcionario);
                    folha.setRubrica(rubrica);
                    folha.setCargo(funcionario.getCargo());
                    folha.setCentroCusto(funcionario.getCentroCusto());
                    folha.setLinhaNegocio(funcionario.getCentroCusto().getLinhaNegocio());
                    folha.setDataInicio(dataInicio);
                    folha.setDataFim(dataFim);
                    folha.setValor(valorBD);
                    folha.setValorTotal(valorBD);
                    folha.setQuantidade(quantidadeBD);
                    folha.setBaseCalculo(baseCalculoBD);
                    folha.setAtivo(true);

                    folhasPagamento.add(folha);
                    
                    logger.debug("Folha de pagamento criada: Funcionário={}, Rubrica={}, Valor={}", 
                               funcionario.getNome(), rubrica.getCodigo(), valorBD);
                } else {
                    logger.debug("Registro já existe para funcionário={}, rubrica={}, período={}", 
                               funcionario.getNome(), rubrica.getCodigo(), dataInicio);
                }
            } else {
                logger.error("Não foi possível extrair valores da string: '{}'", valoresCompletos);
                return;
            }
            
        } catch (StringIndexOutOfBoundsException e) {
            logger.error("Erro de índice ao processar rubrica: {} - Linha: '{}' (comprimento: {})", 
                       e.getMessage(), linha, linha.length());
        } catch (Exception e) {
            logger.error("Erro ao processar rubrica: {} - Linha: '{}'", e.getMessage(), linha);
        }
    }

    private void processarLinhaRegistro(String linha, LocalDate dataInicio, LocalDate dataFim) {
        // Implementar lógica para extrair e salvar rubricas individuais
        // Esta é uma implementação básica - pode ser expandida conforme necessário
        
        if (linha.trim().isEmpty() || linha.startsWith("--") || linha.startsWith("Tipo:") || 
            linha.startsWith("C.Custo:") || linha.startsWith("Salário Base:")) {
            return;
        }

        // Tenta extrair informações de rubrica da linha processada
        // Formato esperado após processamento: "0010 Salário Base           200,00         0,00        13.250,54+"
        
        try {
            String[] partes = linha.trim().split("\\s+");
            if (partes.length >= 5) {
                String codigoRubrica = partes[0];
                String descricaoRubrica = partes[1];
                BigDecimal quantidade = parseBigDecimal(partes[2]);
                BigDecimal baseCalculo = parseBigDecimal(partes[3]);
                BigDecimal valor = parseBigDecimal(partes[4]);
                
                // Busca ou cria rubrica
                Rubrica rubrica = rubricaRepository.findByCodigo(codigoRubrica)
                    .orElseGet(() -> {
                        Rubrica novaRubrica = new Rubrica();
                        novaRubrica.setCodigo(codigoRubrica);
                        novaRubrica.setDescricao(descricaoRubrica);
                        novaRubrica.setTipoRubrica(tipoRubricaRepository.findByDescricao("PROVENTO")
                            .orElseThrow(() -> new RuntimeException("Tipo de rubrica PROVENTO não encontrado")));
                        return rubricaRepository.save(novaRubrica);
                    });

                // Salva registro da folha (implementação básica)
                // Nota: Esta implementação precisa ser expandida para incluir funcionário correto
                logger.debug("Rubrica processada: {} - {} - {}", codigoRubrica, descricaoRubrica, valor);
                
            }
        } catch (Exception e) {
            logger.warn("Erro ao processar linha de rubrica: {}", linha);
        }
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Remove caracteres especiais e limpa o valor
        String valorLimpo = valor.trim()
            .replace("+", "")  // Remove o sinal de +
            .replace("-", "")  // Remove o sinal de -
            .replace(".", "")  // Remove pontos de milhares
            .replace(",", ".") // Substitui vírgula por ponto
            .trim();
        
        // Verifica se o valor resultante é válido
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

    private TipoRubrica determinarTipoRubrica(String tipo) {
        String descricao;

        if (tipo == null || tipo.isEmpty()) {
            descricao = "INFORMATIVO";
        } else if (tipo.equals("+")) {
            descricao = "PROVENTO";
        } else if (tipo.equals("-")) {
            descricao = "DESCONTO";
        } else {
            descricao = "INFORMATIVO";
        }

        return tipoRubricaRepository.findByDescricao(descricao)
                .orElseThrow(() -> new RuntimeException("Tipo de rubrica " + descricao + " não encontrado"));
    }
} 