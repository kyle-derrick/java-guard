package io.kyle.javaguard.compat.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "compat")
public class CompatProperties {
    private String greeting = "unset";
    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
}
