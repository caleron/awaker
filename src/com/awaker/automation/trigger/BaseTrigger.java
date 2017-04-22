package com.awaker.automation.trigger;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Base class for all task triggers.
 */
public abstract class BaseTrigger {
    /**
     * Schedules all schedulable events for the given date of the {@link ZonedDateTime}. Also registers events, if
     * necessary.
     *
     * @param now             current {@link ZonedDateTime}
     * @param runnable        the Task to execute
     * @param executorService the {@link ScheduledExecutorService} that executes the task
     * @return a list of {@link ScheduledFuture}s of the events
     */
    public abstract List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService);

    /**
     * unregisters events if necessary.
     */
    public abstract void unregisterEvents();
}
