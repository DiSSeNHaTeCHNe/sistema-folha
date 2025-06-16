package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
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
        List<String> linhas = lerArquivo(arquivo);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 1; i < linhas.size(); i++) {
            String[] campos = linhas.get(i).split(",");
            if (campos.length < 5) continue;

            String idExterno = campos[0].trim();
            String descricao = campos[1].trim();
            BigDecimal valor = new BigDecimal(campos[2].trim().replace(",", "."));
            LocalDate dataInicio = LocalDate.parse(campos[3].trim(), formatter);
            LocalDate dataFim = campos[4].trim().isEmpty() ? null : LocalDate.parse(campos[4].trim(), formatter);
            String observacao = campos.length > 5 ? campos[5].trim() : null;

            Funcionario funcionario = funcionarioRepository.findByIdExterno(idExterno)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado: " + idExterno));

            Beneficio beneficio = new Beneficio();
            beneficio.setFuncionario(funcionario);
            beneficio.setDescricao(descricao);
            beneficio.setValor(valor);
            beneficio.setDataInicio(dataInicio);
            beneficio.setDataFim(dataFim);
            beneficio.setObservacao(observacao);

            beneficioRepository.save(beneficio);
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
} 