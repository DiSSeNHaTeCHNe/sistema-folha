package br.com.techne.sistemafolha.relatorios.application.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.IntStream;

final class PdfTextTestHelper {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private PdfTextTestHelper() {
    }

    static String extractAllText(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            StringBuilder text = new StringBuilder();
            IntStream.rangeClosed(1, reader.getNumberOfPages())
                .forEach(page -> {
                    try {
                        text.append(new PdfTextExtractor(reader).getTextFromPage(page));
                    } catch (IOException e) {
                        throw new IllegalStateException("Erro ao extrair texto da página " + page, e);
                    }
                });
            reader.close();
            return text.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler PDF", e);
        }
    }

    static String formatCurrency(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(PT_BR);
        return format.format(value != null ? value : BigDecimal.ZERO);
    }

    static boolean containsCurrencyValue(String text, BigDecimal value) {
        String formatted = formatCurrency(value);
        String digitsOnly = formatted.replaceAll("[^0-9]", "");
        return text.contains(formatted)
            || text.replaceAll("[^0-9]", "").contains(digitsOnly);
    }

    static boolean containsEmbeddedImage(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            boolean hasImage = new String(pdf, StandardCharsets.ISO_8859_1).contains("/Subtype/Image");
            reader.close();
            return hasImage;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao ler PDF", e);
        }
    }
}
