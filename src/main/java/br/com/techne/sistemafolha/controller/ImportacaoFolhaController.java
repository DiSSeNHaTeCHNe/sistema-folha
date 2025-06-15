package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.service.ImportacaoFolhaService;
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
public class ImportacaoFolhaController {

    private final ImportacaoFolhaService importacaoFolhaService;

    public ImportacaoFolhaController(ImportacaoFolhaService importacaoFolhaService) {
        this.importacaoFolhaService = importacaoFolhaService;
    }

    @PostMapping("/folha")
    @Operation(summary = "Importa arquivo de folha de pagamento", description = "Importa um arquivo de texto com layout de posições fixas contendo dados da folha de pagamento")
    public ResponseEntity<Void> importarFolha(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            importacaoFolhaService.importarFolha(arquivo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 