package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.ImportacaoFolhaAdpResponseDTO;
import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.service.ImportacaoFolhaAdpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/importacao")
@Tag(name = "Importação", description = "APIs para importação de dados")
public class ImportacaoFolhaAdpController {

    private final ImportacaoFolhaAdpService importacaoFolhaAdpService;

    public ImportacaoFolhaAdpController(ImportacaoFolhaAdpService importacaoFolhaAdpService) {
        this.importacaoFolhaAdpService = importacaoFolhaAdpService;
    }

    @PostMapping("/folha-adp")
    @Operation(summary = "Importa arquivo de folha de pagamento ADP", 
               description = "Importa um arquivo de texto com layout específico do ADP contendo dados da folha de pagamento")
    public ResponseEntity<ImportacaoFolhaAdpResponseDTO> importarFolhaAdp(@RequestParam("arquivo") MultipartFile arquivo) {
        
        try {
            // Validações básicas
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ImportacaoFolhaAdpResponseDTO.error("Arquivo vazio", arquivo.getOriginalFilename()));
            }

            if (!arquivo.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                return ResponseEntity.badRequest()
                    .body(ImportacaoFolhaAdpResponseDTO.error("Formato de arquivo inválido. Use apenas arquivos .txt", arquivo.getOriginalFilename()));
            }

            // Executa a importação
            List<FolhaPagamento> folhasPagamento = importacaoFolhaAdpService.importarFolhaAdp(arquivo);
            
            return ResponseEntity.ok(ImportacaoFolhaAdpResponseDTO.success(
                arquivo.getOriginalFilename(), 
                arquivo.getSize(), 
                folhasPagamento
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ImportacaoFolhaAdpResponseDTO.error("Erro ao importar arquivo ADP: " + e.getMessage(), arquivo.getOriginalFilename()));
        }
    }
} 