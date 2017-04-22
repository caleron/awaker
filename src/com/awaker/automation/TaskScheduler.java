package com.awaker.automation;

import com.awaker.sntp_client.SntpClient;
import com.awaker.util.Log;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

    private List<Task> taskList;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    private ScheduledFuture rescheduleFuture;
    private List<ScheduledFuture> futures = new ArrayList<>();

    public TaskScheduler() {
        taskList = new ArrayList<>();
    }

    public void scheduleAll() {
        //cancel all scheduled tasks before, to avoid double execution of tasks
        for (ScheduledFuture future : futures) {
            future.cancel(false);
        }
        //also unregister to events to avoid double execution, too
        for (Task task : taskList) {
            task.unregisterEvents();
        }

        ZonedDateTime now = SntpClient.getTimeForSure();

        for (Task task : taskList) {
            futures.addAll(task.scheduleForToday(now, executorService));
        }

        if (rescheduleFuture != null) {
            rescheduleFuture.cancel(false);
        }
        //Um 00:01 alle Timer neu setzen
        ZonedDateTime tomorrow = now.plusDays(1).withHour(0).withMinute(1);
        long secondsToMidnight = now.until(tomorrow, ChronoUnit.SECONDS);
        Log.message("Timing rescheduling to " + tomorrow.toString() + "(" + secondsToMidnight + "s remaining)");

        rescheduleFuture = executorService.schedule(this::scheduleAll, secondsToMidnight, TimeUnit.SECONDS);
    }
}
