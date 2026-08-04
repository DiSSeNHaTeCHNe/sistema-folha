package br.com.techne.sistemafolha.relatorios.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "relatorios.geracao")
public class RelatorioGeracaoProperties {

    private int timeoutSegundos = 60;
    private int maxTamanhoMb = 50;
    private int maxJobsSimultaneosPorUsuario = 3;
    private int staleGraceSegundos = 120;
}
