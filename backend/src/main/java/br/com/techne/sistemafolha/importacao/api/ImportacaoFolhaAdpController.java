package br.com.techne.sistemafolha.importacao.api;

import br.com.techne.sistemafolha.folha.domain.FolhaDuplicadaException;
import br.com.techne.sistemafolha.folha.domain.FolhaProcessamentoFalhaException;
import br.com.techne.sistemafolha.importacao.application.ImportacaoFolhaAdpResult;
import br.com.techne.sistemafolha.importacao.application.ImportacaoFolhaAdpService;
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
public class ImportacaoFolhaAdpController {

    private final ImportacaoFolhaAdpService importacaoFolhaAdpService;

    public ImportacaoFolhaAdpController(ImportacaoFolhaAdpService importacaoFolhaAdpService) {
        this.importacaoFolhaAdpService = importacaoFolhaAdpService;
    }

    @PostMapping("/folha-adp")
    @Operation(summary = "Importa arquivo de folha de pagamento ADP",
               description = "Importa um arquivo de texto com layout específico do ADP contendo dados da folha de pagamento")
    public ResponseEntity<ImportacaoFolhaAdpResponseDTO> importarFolhaAdp(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "decimoTerceiro", required = false, defaultValue = "false") Boolean decimoTerceiro,
            @RequestParam(value = "confirmarSubstituicao", required = false, defaultValue = "false") Boolean confirmarSubstituicao) {

        try {
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ImportacaoFolhaAdpResponseDTO.error("Arquivo vazio", arquivo.getOriginalFilename()));
            }

            if (!arquivo.getOriginalFilename().toLowerCase().endsWith(".txt")) {
                return ResponseEntity.badRequest()
                    .body(ImportacaoFolhaAdpResponseDTO.error(
                        "Formato de arquivo inválido. Use apenas arquivos .txt", arquivo.getOriginalFilename()));
            }

            ImportacaoFolhaAdpResult resultado = importacaoFolhaAdpService.importarFolhaAdp(
                arquivo, decimoTerceiro, confirmarSubstituicao);

            return ResponseEntity.ok(ImportacaoFolhaAdpResponseDTO.success(
                arquivo.getOriginalFilename(),
                arquivo.getSize(),
                resultado.folhasPagamento(),
                resultado.processamento()
            ));

        } catch (FolhaDuplicadaException e) {
            return ResponseEntity.status(409)
                .body(ImportacaoFolhaAdpResponseDTO.conflict(
                    e.getMessage(),
                    arquivo.getOriginalFilename(),
                    e.getCompetenciaInicio(),
                    e.getCompetenciaFim(),
                    e.isDecimoTerceiro()
                ));
        } catch (FolhaProcessamentoFalhaException e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.internalServerError()
                .body(ImportacaoFolhaAdpResponseDTO.error(
                    "Falha no processamento da ficha: " + detail, arquivo.getOriginalFilename()));
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.badRequest()
                .body(ImportacaoFolhaAdpResponseDTO.error(
                    "Erro ao importar arquivo ADP: " + message, arquivo.getOriginalFilename()));
        }
    }
}

