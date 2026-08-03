package br.com.techne.sistemafolha.relatorios.application.pdf;

import br.com.techne.sistemafolha.relatorios.application.BrandingTheme;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class RelatorioChartImageFactory {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 200;

    public byte[] lineChart(List<String> labels, List<BigDecimal> values, BrandingTheme theme) {
        if (labels == null || values == null || labels.size() < 2 || values.size() < 2) {
            return new byte[0];
        }
        int count = Math.min(labels.size(), values.size());
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            configureGraphics(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            Color primary = Color.decode(theme.primaryColor());
            Color secondary = Color.decode(theme.secondaryColor());

            double max = values.stream().limit(count).map(v -> v != null ? v.doubleValue() : 0)
                .max(Double::compare).orElse(1.0);
            if (max <= 0) {
                max = 1.0;
            }

            int padding = 30;
            int chartWidth = WIDTH - padding * 2;
            int chartHeight = HEIGHT - padding * 2;
            double stepX = chartWidth / (double) (count - 1);

            int[] xs = new int[count];
            int[] ys = new int[count];
            for (int i = 0; i < count; i++) {
                xs[i] = padding + (int) Math.round(i * stepX);
                double val = values.get(i) != null ? values.get(i).doubleValue() : 0;
                ys[i] = padding + chartHeight - (int) Math.round((val / max) * chartHeight);
            }

            GradientPaint paint = new GradientPaint(0, 0, primary, WIDTH, HEIGHT, secondary);
            g.setPaint(paint);
            g.setStroke(new BasicStroke(2.5f));
            for (int i = 1; i < count; i++) {
                g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            }

            g.setColor(primary);
            for (int i = 0; i < count; i++) {
                g.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            }

            g.setColor(Color.decode(theme.textColor()));
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i < count; i++) {
                String label = labels.get(i);
                int labelWidth = g.getFontMetrics().stringWidth(label);
                g.drawString(label, xs[i] - labelWidth / 2, HEIGHT - 8);
            }
        } finally {
            g.dispose();
        }
        return toPngBytes(image);
    }

    public byte[] horizontalBarChart(Map<String, BigDecimal> data, BrandingTheme theme, int maxBars) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(data.entrySet());
        entries.sort(Comparator.comparing(e -> e.getValue() != null ? e.getValue() : BigDecimal.ZERO));
        int count = Math.min(maxBars, entries.size());
        if (count == 0) {
            return new byte[0];
        }
        List<Map.Entry<String, BigDecimal>> top = entries.subList(entries.size() - count, entries.size());

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            configureGraphics(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            Color primary = Color.decode(theme.primaryColor());
            Color secondary = Color.decode(theme.secondaryColor());
            double max = top.stream()
                .map(e -> e.getValue() != null ? e.getValue().doubleValue() : 0)
                .max(Double::compare).orElse(1.0);
            if (max <= 0) {
                max = 1.0;
            }

            int barHeight = Math.max(12, (HEIGHT - 20) / count - 6);
            int y = 10;
            int labelWidth = 120;
            int barArea = WIDTH - labelWidth - 20;

            for (Map.Entry<String, BigDecimal> entry : top) {
                String label = entry.getKey();
                double val = entry.getValue() != null ? entry.getValue().doubleValue() : 0;
                int barLen = (int) Math.round((val / max) * barArea);

                g.setColor(Color.decode(theme.textColor()));
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                String shortLabel = label.length() > 16 ? label.substring(0, 14) + ".." : label;
                g.drawString(shortLabel, 5, y + barHeight - 2);

                GradientPaint paint = new GradientPaint(labelWidth, y, primary, labelWidth + barLen, y, secondary);
                g.setPaint(paint);
                g.fill(new RoundRectangle2D.Double(labelWidth, y, barLen, barHeight, 8, 8));

                y += barHeight + 6;
            }
        } finally {
            g.dispose();
        }
        return toPngBytes(image);
    }

    private void configureGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
