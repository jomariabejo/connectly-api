package com.jomariabejo.connectly_api.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for Micrometer Metrics and Datadog Integration
 * Enables custom metrics, tagging, and APM tracing capabilities
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfiguration {

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Value("${spring.application.name:connectly-api}")
    private String applicationName;

    /**
     * Customize MeterRegistry with common tags
     * These tags are automatically sent to Datadog
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "service", applicationName,
                        "env", environment,
                        "version", "0.0.1"
                );
    }

    /**
     * Enable @Timed annotation support for method-level metrics
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
