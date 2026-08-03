package br.com.techne.sistemafolha.relatorios.application;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RelatorioBrandingService {

    private final RelatorioBrandingProperties properties;
    private final ResourceLoader resourceLoader;

    public BrandingTheme load() {
        return new BrandingTheme(
            properties.getPrimaryColor(),
            properties.getSecondaryColor(),
            properties.getTextColor(),
            properties.getMutedBackground(),
            loadLogoBytes()
        );
    }

    private Optional<byte[]> loadLogoBytes() {
        String path = properties.getLogoPath();
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                return Optional.empty();
            }
            try (InputStream in = resource.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return bytes.length > 0 ? Optional.of(bytes) : Optional.empty();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
