package br.com.techne.sistemafolha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaFolhaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaFolhaApplication.class, args);
    }
} 