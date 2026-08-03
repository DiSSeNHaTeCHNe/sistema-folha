package br.com.techne.sistemafolha.relatorios.application.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioLayoutHelperTest {

    private RelatorioLayoutHelper helper;
    private BrandingTheme theme;

    @BeforeEach
    void setUp() {
        helper = new RelatorioLayoutHelper();
        theme = new BrandingTheme("#7836FC", "#3661FC", "#273340", "#f8fafc", Optional.empty());
    }

    @Test
    void formatCurrency_usaLocalePtBr() {
        String formatted = helper.formatCurrency(new BigDecimal("1234.56"));
        assertTrue(formatted.contains("R$"));
        assertTrue(formatted.contains("1.234,56") || formatted.contains("1.234,56"));
    }

    @Test
    void formatCurrency_null_retornaZeroFormatado() {
        String formatted = helper.formatCurrency(null);
        assertTrue(formatted.contains("R$"));
    }

    @Test
    void createZebraTable_geraTabelaComCabecalho() {
        var table = helper.createZebraTable(
            List.of("Col A", "Col B"),
            List.of(List.of("1", "2"), List.of("3", "4")),
            theme);

        assertNotNull(table);
        assertEquals(3, table.getRows().size());
    }

    @Test
    void createKpiBox_contemLabelEValor() {
        var table = helper.createKpiBox("Funcionários", "42", theme, 1);
        assertNotNull(table);
        assertEquals(1, table.size());
    }

    @Test
    void createFooterEvent_retornaEventHelper() {
        assertNotNull(helper.createFooterEvent());
    }

    @Test
    void createFooterEvent_paginaComTotalPaginas() throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(helper.createFooterEvent());
            document.open();
            document.add(new Paragraph("Capa"));
            document.newPage();
            document.add(new Paragraph("Conteúdo"));
            document.close();

            PdfReader reader = new PdfReader(out.toByteArray());
            String page2Text = new PdfTextExtractor(reader).getTextFromPage(2);
            reader.close();
            assertTrue(page2Text.contains("Página 2 de 2"));
        }
    }
}
