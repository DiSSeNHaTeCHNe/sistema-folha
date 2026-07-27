package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.beneficios.application.ImportacaoBeneficioMensalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/importacao")
@Tag(name = "Importação", description = "APIs para importação de dados")
public class ImportacaoBeneficioMensalController {

    private final ImportacaoBeneficioMensalService importacaoBeneficioMensalService;

    public ImportacaoBeneficioMensalController(ImportacaoBeneficioMensalService importacaoBeneficioMensalService) {
        this.importacaoBeneficioMensalService = importacaoBeneficioMensalService;
    }

    @PostMapping("/beneficios-mensais")
    @Operation(
            summary = "Importa arquivo de benefícios mensais",
            description = "Importa um arquivo .xlsx com aba Lancamentos contendo lançamentos de benefícios mensais")
    public ResponseEntity<ImportacaoResultadoDTO> importarBeneficiosMensais(
            @RequestParam("file") MultipartFile file,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            @RequestParam(required = false, defaultValue = "false") Boolean confirmar) {
        try {
            ImportacaoResultadoDTO resultado = importacaoBeneficioMensalService.importar(
                    file, competenciaInicio, competenciaFim, confirmar);
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            throw new IllegalArgumentException("Arquivo inválido ou corrompido: " + e.getMessage());
        }
    }
}
