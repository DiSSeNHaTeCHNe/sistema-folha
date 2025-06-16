package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.RubricaRepository;
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

    private final FuncionarioRepository funcionarioRepository;
    private final RubricaRepository rubricaRepository;
    private final FolhaPagamentoRepository folhaPagamentoRepository;

    private static final Pattern COMPETENCIA_PATTERN = Pattern.compile("Competência:\\s*(\\d{2}/\\d{2}/\\d{4})\\s*a\\s*(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern FUNCIONARIO_PATTERN = Pattern.compile("(\\d+)\\s+([A-Z\\s]+)");
    private static final Pattern RUBRICA_PATTERN = Pattern.compile("(\\d{4})\\s+([A-Z\\s]+)\\s+(\\d+,\\d+)\\s+(\\d+,\\d+)\\s+([\\d.,]+)([+-])?");

    private static class DadosFuncionario {
        String idExterno;
        String nome;
    }

    public ImportacaoFolhaService(
            FuncionarioRepository funcionarioRepository,
            RubricaRepository rubricaRepository,
            FolhaPagamentoRepository folhaPagamentoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.rubricaRepository = rubricaRepository;
        this.folhaPagamentoRepository = folhaPagamentoRepository;
    }

    @Transactional
    public void importarFolha(MultipartFile arquivo) throws IOException {
        List<String> linhas = lerArquivo(arquivo);
        LocalDate dataInicio = null;
        LocalDate dataFim = null;
        DadosFuncionario dadosFuncionario = new DadosFuncionario();

        for (String linha : linhas) {
            // Procura período de competência
            Matcher competenciaMatcher = COMPETENCIA_PATTERN.matcher(linha);
            if (competenciaMatcher.find()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                dataInicio = LocalDate.parse(competenciaMatcher.group(1), formatter);
                dataFim = LocalDate.parse(competenciaMatcher.group(2), formatter);
                continue;
            }

            // Procura dados do funcionário
            Matcher funcionarioMatcher = FUNCIONARIO_PATTERN.matcher(linha);
            if (funcionarioMatcher.find()) {
                dadosFuncionario.idExterno = funcionarioMatcher.group(1);
                dadosFuncionario.nome = funcionarioMatcher.group(2).trim();
                continue;
            }

            // Procura rubricas
            Matcher rubricaMatcher = RUBRICA_PATTERN.matcher(linha);
            if (rubricaMatcher.find() && dadosFuncionario.idExterno != null && dataInicio != null && dataFim != null) {
                String codigoRubrica = rubricaMatcher.group(1);
                String descricaoRubrica = rubricaMatcher.group(2).trim();
                BigDecimal quantidade = new BigDecimal(rubricaMatcher.group(3).replace(",", "."));
                BigDecimal baseCalculo = new BigDecimal(rubricaMatcher.group(4).replace(",", "."));
                BigDecimal valor = new BigDecimal(rubricaMatcher.group(5).replace(".", "").replace(",", "."));
                String tipo = rubricaMatcher.group(6);

                // Busca ou cria rubrica
                Rubrica rubrica = rubricaRepository.findByCodigo(codigoRubrica)
                    .orElseGet(() -> {
                        Rubrica novaRubrica = new Rubrica();
                        novaRubrica.setCodigo(codigoRubrica);
                        novaRubrica.setDescricao(descricaoRubrica);
                        novaRubrica.setTipo(determinarTipoRubrica(tipo));
                        return rubricaRepository.save(novaRubrica);
                    });

                // Busca funcionário
                Funcionario funcionario = funcionarioRepository.findByIdExterno(dadosFuncionario.idExterno)
                    .orElseThrow(() -> new RuntimeException("Funcionário não encontrado: " + dadosFuncionario.idExterno));

                // Cria registro da folha
                FolhaPagamento folha = new FolhaPagamento();
                folha.setFuncionario(funcionario);
                folha.setRubrica(rubrica);
                folha.setDataInicio(dataInicio);
                folha.setDataFim(dataFim);
                folha.setValor(valor);
                folha.setQuantidade(quantidade);
                folha.setBaseCalculo(baseCalculo);

                folhaPagamentoRepository.save(folha);
            }
        }
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

    private Rubrica.TipoRubrica determinarTipoRubrica(String tipo) {
        if (tipo == null) {
            return Rubrica.TipoRubrica.INFORMATIVO;
        }
        return tipo.equals("+") ? Rubrica.TipoRubrica.PROVENTO : Rubrica.TipoRubrica.DESCONTO;
    }
} 