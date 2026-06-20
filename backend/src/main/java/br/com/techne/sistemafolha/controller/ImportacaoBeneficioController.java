package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.service.ImportacaoBeneficioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/importacao")
@Tag(name = "Importação", description = "APIs para importação de dados")
public class ImportacaoBeneficioController {

    private final ImportacaoBeneficioService importacaoBeneficioService;

    public ImportacaoBeneficioController(ImportacaoBeneficioService importacaoBeneficioService) {
        this.importacaoBeneficioService = importacaoBeneficioService;
    }

    @PostMapping("/beneficios")
    @Operation(summary = "Importa arquivo de benefícios", description = "Importa um arquivo CSV contendo dados de benefícios dos funcionários")
    public ResponseEntity<Void> importarBeneficios(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            importacaoBeneficioService.importarBeneficios(arquivo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 