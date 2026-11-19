package com.company.helpdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Memanfaatkan Virtual Threads Java 25 (spring.threads.virtual.enabled=true)
}