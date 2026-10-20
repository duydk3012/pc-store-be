package com.datn.pc_store.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.cleanup.eanabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
