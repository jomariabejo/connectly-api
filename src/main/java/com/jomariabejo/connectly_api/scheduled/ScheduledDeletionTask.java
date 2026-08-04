package com.jomariabejo.connectly_api.scheduled;

import com.jomariabejo.connectly_api.service.UserService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to automatically delete user accounts that have reached their scheduled deletion date.
 * This runs daily at 2 AM (02:00) by default.
 */
@Component
@EnableScheduling
public class ScheduledDeletionTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledDeletionTask.class);

    private final UserService userService;

    public ScheduledDeletionTask(UserService userService) {
        this.userService = userService;
    }

    /**
     * Runs daily at 02:00 (2 AM) to check for and permanently delete users
     * whose grace period has expired.
     * 
     * Cron expression: "0 2 * * *" means:
     * - 0: at minute 0
     * - 2: at hour 2 (2 AM)
     * - *: every day
     * - *: every month
     * - *: every day of week
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "permanentlyDeleteScheduledUsers", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void permanentlyDeleteScheduledUsers() {
        logger.info("Starting scheduled deletion task...");
        
        try {
            int deletedCount = userService.checkAndDeleteScheduledUsers();
            logger.info("Successfully permanently deleted {} user account(s)",  deletedCount);
        } catch (Exception e) {
            logger.error("Error during scheduled deletion task", e);
        }
    }

    /**
     * Alternative scheduled task that can be invoked more frequently for testing.
     * Runs every hour at minute 0.
     * DISABLED by default - uncomment to enable.
     */
    /*
    @Scheduled(cron = "0 * * * *")
    public void checkScheduledDeletionsHourly() {
        logger.debug("Running hourly check for scheduled deletions...");
        
        try {
            int deletedCount = userService.checkAndDeleteScheduledUsers();
            if (deletedCount > 0) {
                logger.info("Deleted {} user(s) during hourly check", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Error during hourly deletion check", e);
        }
    }
    */
}
