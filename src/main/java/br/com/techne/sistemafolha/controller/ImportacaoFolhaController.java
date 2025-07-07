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

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> importarFolha(@RequestParam("arquivo") MultipartFile arquivo) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validações básicas
            if (arquivo.isEmpty()) {
                response.put("success", false);
                response.put("message", "Arquivo vazio");
                return ResponseEntity.badRequest().body(response);
            }

            if (!arquivo.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                response.put("success", false);
                response.put("message", "Formato de arquivo inválido. Use apenas arquivos .txt");
                return ResponseEntity.badRequest().body(response);
            }

            // Executa a importação
            importacaoFolhaService.importarFolha(arquivo);
            
            response.put("success", true);
            response.put("message", "Arquivo importado com sucesso");
            response.put("arquivo", arquivo.getOriginalFilename());
            response.put("tamanho", arquivo.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao importar arquivo: " + e.getMessage());
            response.put("arquivo", arquivo.getOriginalFilename());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 