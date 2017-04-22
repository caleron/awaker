package com.awaker.automation.tasks;

/**
 * Base class for task actions.
 */
public abstract class BaseTaskAction implements Runnable {
    private int id;

    public BaseTaskAction(int id) {
        this.id = id;
    }

}
