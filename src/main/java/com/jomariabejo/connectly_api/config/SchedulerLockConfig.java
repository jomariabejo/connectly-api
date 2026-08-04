package com.jomariabejo.connectly_api.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Distributed locking for the {@code @Scheduled} jobs.
 *
 * <p>Both background tasks used plain Spring scheduling, so every replica ran every job — two
 * instances meant two concurrent account-deletion sweeps at 02:00. Each job now takes a row lock in
 * the {@code shedlock} table (created by the V1 migration) and only one instance proceeds.
 *
 * <p>Harmless when running a single instance: the lock is simply always acquired.
 *
 * @see com.jomariabejo.connectly_api.scheduled.ScheduledDeletionTask
 * @see com.jomariabejo.connectly_api.scheduled.PasswordResetTokenCleanupTask
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
