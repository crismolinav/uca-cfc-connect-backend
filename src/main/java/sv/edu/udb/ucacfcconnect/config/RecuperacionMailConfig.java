package sv.edu.udb.ucacfcconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RecuperacionMailConfig {
    @Bean(name = "recuperacionMailExecutor")
    public ThreadPoolTaskExecutor recuperacionMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("recuperacion-mail-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
