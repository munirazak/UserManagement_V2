package com.kopibru.usermanagement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SuperAdminBootstrapProperties.class)
public class AppConfig {
}
