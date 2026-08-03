package br.com.techne.sistemafolha.relatorios.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "relatorios.branding")
public class RelatorioBrandingProperties {

    private String primaryColor = "#7836FC";
    private String secondaryColor = "#3661FC";
    private String textColor = "#273340";
    private String mutedBackground = "#f8fafc";
    private String logoPath = "classpath:branding/logo.png";
}
