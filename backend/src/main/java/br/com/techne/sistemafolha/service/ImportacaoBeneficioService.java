package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
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

@Service
public class ImportacaoBeneficioService {

    private static final Logger logger = LoggerFactory.getLogger(ImportacaoBeneficioService.class);

    private final FuncionarioRepository funcionarioRepository;
    private final BeneficioRepository beneficioRepository;

    public ImportacaoBeneficioService(
            FuncionarioRepository funcionarioRepository,
            BeneficioRepository beneficioRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.beneficioRepository = beneficioRepository;
    }

    @Transactional
    public void importarBeneficios(MultipartFile arquivo) throws IOException {
        logger.info("Iniciando importação de benefícios - Arquivo: {}, Tamanho: {} bytes", 
                   arquivo.getOriginalFilename(), arquivo.getSize());
        
        List<String> linhas = lerArquivo(arquivo);
        logger.debug("Arquivo lido com {} linhas", linhas.size());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int registrosProcessados = 0;
        int linhasIgnoradas = 0;
        
        for (int i = 1; i < linhas.size(); i++) {
            String[] campos = linhas.get(i).split(",");
            if (campos.length < 5) {
                logger.warn("Linha {} ignorada - campos insuficientes: {}", i + 1, campos.length);
                linhasIgnoradas++;
                continue;
            }

            String idExterno = campos[0].trim();
            String descricao = campos[1].trim();
            BigDecimal valor = new BigDecimal(campos[2].trim().replace(",", "."));
            LocalDate dataInicio = LocalDate.parse(campos[3].trim(), formatter);
            LocalDate dataFim = campos[4].trim().isEmpty() ? null : LocalDate.parse(campos[4].trim(), formatter);
            String observacao = campos.length > 5 ? campos[5].trim() : null;

            logger.debug("Processando benefício: Funcionário={}, Descrição={}, Valor={}, Data Início={}", 
                       idExterno, descricao, valor, dataInicio);

            Funcionario funcionario = funcionarioRepository.findByIdExterno(idExterno)
                .orElseThrow(() -> {
                    logger.error("Funcionário não encontrado: ID={}", idExterno);
                    return new RuntimeException("Funcionário não encontrado: " + idExterno);
                });

            Beneficio beneficio = new Beneficio();
            beneficio.setFuncionario(funcionario);
            beneficio.setDescricao(descricao);
            beneficio.setValor(valor);
            beneficio.setDataInicio(dataInicio);
            beneficio.setDataFim(dataFim);
            beneficio.setObservacao(observacao);

            beneficioRepository.save(beneficio);
            registrosProcessados++;
            
            logger.debug("Benefício salvo: Funcionário={}, Descrição={}, Valor={}", 
                       funcionario.getNome(), descricao, valor);
        }
        
        logger.info("Importação de benefícios concluída - Registros processados: {}, Linhas ignoradas: {}", 
                   registrosProcessados, linhasIgnoradas);
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
} 