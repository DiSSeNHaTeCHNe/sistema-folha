package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.model.TipoRubrica;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportacaoFolhaService {

    private static final Logger logger = LoggerFactory.getLogger(ImportacaoFolhaService.class);

    private final FuncionarioRepository funcionarioRepository;
    private final RubricaRepository rubricaRepository;
    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final TipoRubricaRepository tipoRubricaRepository;

    // Padrões para extrair informações do arquivo
    private static final Pattern COMPETENCIA_PATTERN = Pattern.compile("Competência:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*a\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern FUNCIONARIO_PATTERN = Pattern.compile("(\\d+)\\s+([A-Z\\s\\-]+)\\s+(\\d+)\\s+([A-Z\\s]+)");
    private static final Pattern RUBRICA_PATTERN = Pattern.compile("(\\d{4})\\s+([A-Z\\s\\-]+)\\s+(\\d+,\\d+)\\s+(\\d+,\\d+)\\s+([\\d.,]+)([+-])?");
    private static final Pattern CENTRO_CUSTO_PATTERN = Pattern.compile("(\\d+)\\s+([A-Z\\s\\-]+)");
    


    private static class DadosFuncionario {
        String idExterno;
        String nome;
        String centroCusto;
    }

    public ImportacaoFolhaService(
            FuncionarioRepository funcionarioRepository,
            RubricaRepository rubricaRepository,
            FolhaPagamentoRepository folhaPagamentoRepository,
            TipoRubricaRepository tipoRubricaRepository
    ) {
        this.funcionarioRepository = funcionarioRepository;
        this.rubricaRepository = rubricaRepository;
        this.folhaPagamentoRepository = folhaPagamentoRepository;
        this.tipoRubricaRepository = tipoRubricaRepository;
    }

    @Transactional
    public void importarFolha(MultipartFile arquivo) throws IOException {
        logger.info("Iniciando importação de folha de pagamento - Arquivo: {}, Tamanho: {} bytes", 
                   arquivo.getOriginalFilename(), arquivo.getSize());
        
        List<String> linhas = lerArquivo(arquivo);
        logger.debug("Arquivo lido com {} linhas", linhas.size());
        
        LocalDate dataInicio = null;
        LocalDate dataFim = null;
        DadosFuncionario dadosFuncionario = new DadosFuncionario();
        int[] contadores = {0, 0, 0}; // [registrosProcessados, funcionariosProcessados, rubricasCriadas]

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            
            // Procura período de competência
            Matcher competenciaMatcher = COMPETENCIA_PATTERN.matcher(linha);
            if (competenciaMatcher.find()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    dataInicio = LocalDate.parse(competenciaMatcher.group(1), formatter);
                    dataFim = LocalDate.parse(competenciaMatcher.group(2), formatter);
                    logger.info("Período de competência identificado: {} a {}", dataInicio, dataFim);
                } catch (Exception e) {
                    logger.error("Erro ao processar período de competência na linha {}: {}", i + 1, linha);
                }
                continue;
            }

            // Procura dados do funcionário (formato: 258 SERVICOS - EDU 273 RENATO AMANCIO DA SILVA)
            // Primeiro tenta o padrão completo
            Matcher funcionarioMatcher = FUNCIONARIO_PATTERN.matcher(linha);
            if (funcionarioMatcher.find()) {
                try {
                    dadosFuncionario.centroCusto = funcionarioMatcher.group(1) + " " + funcionarioMatcher.group(2).trim();
                    dadosFuncionario.idExterno = funcionarioMatcher.group(3);
                    dadosFuncionario.nome = funcionarioMatcher.group(4).trim();
                    logger.debug("Funcionário identificado: Centro={}, ID={}, Nome={}", 
                               dadosFuncionario.centroCusto, dadosFuncionario.idExterno, dadosFuncionario.nome);
                    contadores[1]++;
                } catch (Exception e) {
                    logger.error("Erro ao processar dados do funcionário na linha {}: {}", i + 1, linha);
                }
                continue;
            }

            // Se não encontrou o padrão completo, tenta apenas o centro de custo
            Matcher centroCustoMatcher = CENTRO_CUSTO_PATTERN.matcher(linha);
            if (centroCustoMatcher.find() && linha.contains("SERVICOS") || linha.contains("SUPORTE") || linha.contains("P&D")) {
                try {
                    dadosFuncionario.centroCusto = centroCustoMatcher.group(1) + " " + centroCustoMatcher.group(2).trim();
                    logger.debug("Centro de custo identificado: {}", dadosFuncionario.centroCusto);
                } catch (Exception e) {
                    logger.error("Erro ao processar centro de custo na linha {}: {}", i + 1, linha);
                }
                continue;
            }

            // Procura rubricas (formato: 0010 Salário Base 200,00 0,00 13.250,54+)
            Matcher rubricaMatcher = RUBRICA_PATTERN.matcher(linha);
            if (rubricaMatcher.find() && dadosFuncionario.idExterno != null && dataInicio != null && dataFim != null) {
                try {
                    String codigoRubrica = rubricaMatcher.group(1);
                    String descricaoRubrica = rubricaMatcher.group(2).trim();
                    BigDecimal quantidade = parseBigDecimal(rubricaMatcher.group(3));
                    BigDecimal baseCalculo = parseBigDecimal(rubricaMatcher.group(4));
                    BigDecimal valor = parseBigDecimal(rubricaMatcher.group(5));
                    String tipo = rubricaMatcher.group(6);

                    logger.debug("Processando rubrica: Código={}, Descrição={}, Valor={}, Funcionário={}", 
                               codigoRubrica, descricaoRubrica, valor, dadosFuncionario.nome);

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
                            contadores[2]++;
                            return rubricaRepository.save(novaRubrica);
                        });

                    // Busca funcionário
                    Funcionario funcionario = funcionarioRepository.findByIdExterno(dadosFuncionario.idExterno)
                        .orElseThrow(() -> {
                            logger.error("Funcionário não encontrado: ID={}, Nome={}", 
                                       dadosFuncionario.idExterno, dadosFuncionario.nome);
                            return new RuntimeException("Funcionário não encontrado: " + dadosFuncionario.idExterno + " - " + dadosFuncionario.nome);
                        });

                    // Verifica se já existe registro para este funcionário, rubrica e período
                    if (!folhaPagamentoRepository.existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
                            funcionario.getId(), rubrica.getId(), dataInicio, dataFim)) {
                        
                        // Cria registro da folha
                        FolhaPagamento folha = new FolhaPagamento();
                        folha.setFuncionario(funcionario);
                        folha.setRubrica(rubrica);
                        folha.setCargo(funcionario.getCargo());
                        folha.setCentroCusto(funcionario.getCentroCusto());
                        folha.setLinhaNegocio(funcionario.getCentroCusto().getLinhaNegocio());
                        folha.setDataInicio(dataInicio);
                        folha.setDataFim(dataFim);
                        folha.setValor(valor);
                        folha.setQuantidade(quantidade);
                        folha.setBaseCalculo(baseCalculo);

                        folhaPagamentoRepository.save(folha);
                        contadores[0]++;
                        
                        logger.debug("Registro de folha salvo: Funcionário={}, Rubrica={}, Valor={}", 
                                   funcionario.getNome(), rubrica.getCodigo(), valor);
                    } else {
                        logger.debug("Registro já existe para funcionário={}, rubrica={}, período={}", 
                                   funcionario.getNome(), rubrica.getCodigo(), dataInicio);
                    }
                } catch (Exception e) {
                    logger.error("Erro ao processar rubrica na linha {}: {} - Erro: {}", i + 1, linha, e.getMessage());
                }
            }
        }
        
        logger.info("Importação de folha concluída - Registros processados: {}, Funcionários: {}, Rubricas criadas: {}", 
                   contadores[0], contadores[1], contadores[2]);
    }

    private List<String> lerArquivo(MultipartFile arquivo) throws IOException {
        List<String> linhas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream()))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                linhas.add(linha);
            }
        }
        return linhas;
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Remove pontos de milhares e substitui vírgula por ponto
        String valorLimpo = valor.replace(".", "").replace(",", ".").trim();
        
        try {
            return new BigDecimal(valorLimpo);
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor: {}", valor);
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