package com.awaker.automation.trigger;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimeIntervalTrigger extends BaseTrigger {
    private final int intervalSeconds;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeIntervalTrigger(int intervalSeconds) {
        this(intervalSeconds, LocalTime.MIN, LocalTime.MAX);
    }

    public TimeIntervalTrigger(int intervalSeconds, LocalTime startTime, LocalTime endTime) {
        this.intervalSeconds = intervalSeconds;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService) {
        ArrayList<ScheduledFuture> list = new ArrayList<>();
        //create datetimes of the given times
        ZonedDateTime planTime = now.with(startTime);
        ZonedDateTime endDateTime = now.with(endTime);

        while (planTime.isBefore(endDateTime)) {
            //schedule the event if its time has not passed yet
            if (now.isBefore(planTime)) {
                long secondsToStart = now.until(planTime, ChronoUnit.SECONDS);
                list.add(executorService.schedule(runnable, secondsToStart, TimeUnit.SECONDS));
            }
            //add interval to planTime again
            planTime = planTime.plus(intervalSeconds, ChronoUnit.SECONDS);
        }

        return list;
    }

    @Override
    public void unregisterEvents() {

    }
}
