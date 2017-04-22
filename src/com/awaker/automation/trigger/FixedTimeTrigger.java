package com.awaker.automation.trigger;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FixedTimeTrigger extends BaseTrigger {
    private final LocalTime time;

    public FixedTimeTrigger(LocalTime time) {
        this.time = time;
    }

    public FixedTimeTrigger(int hours, int minutes, int seconds) {
        time = LocalTime.of(hours, minutes, seconds);
    }

    public List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService) {
        long secondsToEvent = now.until(time, ChronoUnit.SECONDS);

        List<ScheduledFuture> list = new ArrayList<>();
        list.add(executorService.schedule(runnable, secondsToEvent, TimeUnit.SECONDS));

        return list;
    }
}
