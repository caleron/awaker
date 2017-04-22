package com.awaker.automation;

import com.awaker.automation.tasks.BaseTaskAction;
import com.awaker.automation.trigger.BaseTrigger;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Task that contains a trigger and an action.
 */
public class Task {
    private BaseTrigger trigger;
    private BaseTaskAction action;

    public Task(BaseTrigger trigger, BaseTaskAction action) {
        this.trigger = trigger;
        this.action = action;
    }

    public List<ScheduledFuture> scheduleForToday(ZonedDateTime now, ScheduledExecutorService executorService) {
        return trigger.scheduleForToday(now, action, executorService);
    }

    public void unregisterEvents() {
        trigger.unregisterEvents();
    }

    public BaseTrigger getTrigger() {
        return trigger;
    }

    public BaseTaskAction getAction() {
        return action;
    }
}
