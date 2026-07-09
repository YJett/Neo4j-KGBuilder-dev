package com.warmer.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {
    private boolean enabled = true;
    private boolean scheduled = true;
    private long fixedDelayMs = 600000L;
    private long initialDelayMs = 120000L;
    private long incrementalLookbackMinutes = 20L;
    private Mysql mysql = new Mysql();

    @Data
    public static class Mysql {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private int maximumPoolSize = 5;
        private int minimumIdle = 1;
    }
}
