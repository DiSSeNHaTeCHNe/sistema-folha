package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
public class RelatorioLayoutHelper {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String FOOTER_TEXT = "Gerado pelo Sistema de Folha — Techne";

    public String formatCurrency(BigDecimal value) {
        if (value == null) {
            return formatCurrency(BigDecimal.ZERO);
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(PT_BR);
        return format.format(value);
    }

    public Font titleFont(BrandingTheme theme) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, parseColor(theme.textColor()));
    }

    public Font bodyFont(BrandingTheme theme) {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, parseColor(theme.textColor()));
    }

    public Font kpiValueFont(BrandingTheme theme) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, parseColor(theme.secondaryColor()));
    }

    public Font kpiLabelFont(BrandingTheme theme) {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, parseColor(theme.textColor()));
    }

    public Font footerFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
    }

    public PdfPTable createKpiBox(String label, String value, BrandingTheme theme, int colspan) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(1);
        cell.setBorderColor(parseColor(theme.primaryColor()));
        cell.setBackgroundColor(parseColor(theme.mutedBackground()));
        cell.setPadding(8);
        cell.addElement(new Phrase(label, kpiLabelFont(theme)));
        cell.addElement(new Phrase(value, kpiValueFont(theme)));
        cell.setColspan(colspan);
        table.addCell(cell);
        return table;
    }

    public PdfPTable createZebraTable(List<String> headers, List<List<String>> rows, BrandingTheme theme) {
        int cols = headers.size();
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(8);

        Color headerBg = parseColor(theme.primaryColor());
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        Color zebra = new Color(241, 245, 249);
        Font rowFont = bodyFont(theme);
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            for (String col : row) {
                PdfPCell cell = new PdfPCell(new Phrase(col != null ? col : "", rowFont));
                cell.setPadding(6);
                if (i % 2 == 1) {
                    cell.setBackgroundColor(zebra);
                }
                table.addCell(cell);
            }
        }
        return table;
    }

    public PdfPageEventHelper createFooterEvent() {
        return new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                if (writer.getPageNumber() <= 1) {
                    return;
                }
                Phrase footer = new Phrase(
                    FOOTER_TEXT + " | Página " + writer.getPageNumber(),
                    footerFont());
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    footer,
                    (document.left() + document.right()) / 2,
                    document.bottom() - 10,
                    0);
            }
        };
    }

    public Color parseColor(String hex) {
        return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
    }
}
