package io.kyle.javaguard.compat;
import io.kyle.javaguard.compat.config.CompatProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@EnableConfigurationProperties(CompatProperties.class)
@EnableCaching @EnableAsync
public class CompatApplication {
    public static final int EXPECTED_MAJOR = 61;
    public static void main(String[] args) { SpringApplication.run(CompatApplication.class, args); }
}
