package com.awaker.automation.trigger;

import com.awaker.global.router.EventReceiver;
import com.awaker.global.router.EventRouter;
import com.awaker.global.router.GlobalEvent;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by Patrick on 22.04.2017.
 */
public class GlobalEventTrigger extends BaseTrigger implements EventReceiver {
    private final GlobalEvent event;
    private Runnable runnable;
    private final boolean synchronous;

    public GlobalEventTrigger(GlobalEvent event, boolean synchronous) {
        this.event = event;
        this.synchronous = synchronous;
    }

    /**
     * Creates a {@link GlobalEventTrigger} that runs the task asynchronously.
     *
     * @param event
     */
    public GlobalEventTrigger(GlobalEvent event) {
        this(event, false);
    }

    @Override
    public List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService) {
        this.runnable = runnable;
        EventRouter.registerReceiver(this, event);
        return null;
    }

    @Override
    public void unregisterEvents() {
        EventRouter.unregisterReceiver(this, event);
    }

    @Override
    public void receiveGlobalEvent(GlobalEvent globalEvent) {
        if (synchronous) {
            runnable.run();
        } else {
            new Thread(runnable).start();
        }
    }
}
