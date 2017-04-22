package com.awaker.automation.trigger;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Base class for all task triggers.
 */
public abstract class BaseTrigger {
    public abstract List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService);
}
