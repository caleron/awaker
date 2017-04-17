package com.awaker.global;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

/**
 * Central class for observing user activity.
 */
public class UserActivityCenter {
    /**
     * Private default constructor, so nobody can create an instance.
     */
    private UserActivityCenter() {
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
            reportActivity(source);
        } else {
            reportActivity(source.getClass());
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
}
