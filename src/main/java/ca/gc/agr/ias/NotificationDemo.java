package ca.gc.agr.ias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
public class NotificationDemo {
    

    public static void main(String[] args) {
        SpringApplication.run(NotificationDemo.class, args);
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setQueueCapacity(5);
        executor.setMaxPoolSize(10);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);

        return executor;
    }
}
