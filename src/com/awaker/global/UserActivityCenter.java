package com.awaker.global;

import com.awaker.global.router.CommandRouter;
import com.awaker.global.router.EventRouter;
import com.awaker.global.router.GlobalEvent;
import com.awaker.server.json.Answer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central class for observing user activity.
 */
public class UserActivityCenter {

    /**
     * True, if no command was received for some time (20mins atm) and music is not playing.
     */
    private static boolean isIdle;

    /**
     * Private default constructor, so nobody can create an instance.
     */
    private UserActivityCenter() {
    }

    static {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        //check every 5 minutes if server is idle
        executorService.scheduleWithFixedDelay(UserActivityCenter::checkForIdle, 20, 5, TimeUnit.MINUTES);
    }

    private static HashMap<Class, Instant> lastActivities = new HashMap<>();

    /**
     * Reports an activity from the given source class.
     *
     * @param source The source class where the activity was registered.
     */
    public static void reportActivity(Class source) {
        lastActivities.put(source, Instant.now());
    }

    /**
     * Reports an activity from the given source object.
     *
     * @param source The source object where the activity was registered.
     */
    public static void reportActivity(Object source) {
        if (source instanceof Class) {
            lastActivities.put((Class) source, Instant.now());
        } else {
            reportActivity(source.getClass());
        }

        //set isIdle to false and fire event, if idle
        if (isIdle) {
            isIdle = false;
            EventRouter.raiseEvent(GlobalEvent.ACTIVE_AFTER_IDLE);
        }
    }


    /**
     * Returns the last registered user activity time.
     *
     * @return {@link Instant} of the user last activity time.
     */
    public static Instant getLastUserActivity() {
        Optional<Instant> max = lastActivities.values().stream().max(Instant::compareTo);
        return max.orElse(Instant.EPOCH);
    }

    /**
     * Returns true if the server is idle for over 20 mins
     *
     * @return true if the server is idle for over 20 mins
     */
    public static boolean isIdle() {
        return isIdle;
    }

    /**
     * Checks if the last activity was more than 20 minutes ago and music is not playing. If both of them are true, the
     * IDLE event is fired
     */
    private static void checkForIdle() {
        //fire IDLE event after 20 mins inactivity and not playing
        if (!isIdle && Duration.between(getLastUserActivity(), Instant.now()).getSeconds() > 1200) {
            Answer answer = CommandRouter.handleCommand(DataCommand.GET_STATUS, true);
            if (!answer.playing) {
                isIdle = true;
                EventRouter.raiseEvent(GlobalEvent.IDLE);
            }
        }
    }
}
