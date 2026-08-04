package br.com.techne.sistemafolha.relatorios.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Configuration
@EnableAsync
public class RelatorioAsyncConfig {

    @Bean(name = "relatorioExecutor")
    public Executor relatorioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("relatorio-");
        executor.initialize();
        return executor;
    }

    @Bean
    Consumer<Long> relatorioEnqueueFn(RelatorioGeracaoService relatorioGeracaoService) {
        return relatorioGeracaoService::enfileirarProcessamento;
    }
}
