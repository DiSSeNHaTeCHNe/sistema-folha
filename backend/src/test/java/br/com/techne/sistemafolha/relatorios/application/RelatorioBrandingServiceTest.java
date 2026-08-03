package br.com.techne.sistemafolha.relatorios.application;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioBrandingServiceTest {

    @Test
    void load_comLogoPresente_retornaBytesNonEmpty() {
        RelatorioBrandingProperties props = new RelatorioBrandingProperties();
        props.setPrimaryColor("#7836FC");
        props.setSecondaryColor("#3661FC");
        props.setTextColor("#273340");
        props.setMutedBackground("#f8fafc");
        props.setLogoPath("classpath:branding/logo.png");

        ResourceLoader loader = new DefaultResourceLoader();
        RelatorioBrandingService service = new RelatorioBrandingService(props, loader);

        BrandingTheme theme = service.load();

        assertEquals("#7836FC", theme.primaryColor());
        assertEquals("#3661FC", theme.secondaryColor());
        assertEquals("#273340", theme.textColor());
        assertEquals("#f8fafc", theme.mutedBackground());
        assertTrue(theme.logoBytes().isPresent());
        assertTrue(theme.logoBytes().get().length > 0);
    }

    @Test
    void load_logoAusente_retornaFallbackSemExcecao() {
        RelatorioBrandingProperties props = new RelatorioBrandingProperties();
        props.setLogoPath("classpath:branding/inexistente.png");

        ResourceLoader loader = new DefaultResourceLoader();
        RelatorioBrandingService service = new RelatorioBrandingService(props, loader);

        BrandingTheme theme = service.load();

        assertFalse(theme.logoBytes().isPresent());
        assertEquals("#7836FC", theme.primaryColor());
    }
}
