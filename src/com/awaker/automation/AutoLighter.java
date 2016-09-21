package com.awaker.automation;

import com.awaker.util.Log;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoLighter {
    private final EnvironmentEventListener listener;
    private SunriseSunsetCalculator calculator;
    private ZonedDateTime sunrise;
    private ZonedDateTime sunset;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> sunriseSchedule, sunsetSchedule, midnightSchedule;

    public AutoLighter(EnvironmentEventListener listener) {
        this.listener = listener;
    }

    public static void start(EnvironmentEventListener listener) {
        new Thread(new AutoLighter(listener)::init).start();
    }

    private void init() {
        Location location = new Location("53.4842682", "10.1460763");
        calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
        refreshTimes();
        scheduleEvents();
    }

    @SuppressWarnings("Duplicates")
    private void scheduleEvents() {
        ZonedDateTime now = ZonedDateTime.now();

        if (now.isBefore(sunrise)) {
            if (sunriseSchedule != null) {
                sunriseSchedule.cancel(false);
            }

            long secondsToSunrise = now.until(sunrise, ChronoUnit.SECONDS); //Duration.between(sunrise, now).getSeconds();
            Log.message("Timing sunrise to " + sunrise.toString() + " (" + secondsToSunrise + "s remaining)");
            sunriseSchedule = executor.schedule(listener::sunrise, secondsToSunrise, TimeUnit.SECONDS);
        }

        if (now.isBefore(sunset)) {
            if (sunsetSchedule != null) {
                sunsetSchedule.cancel(false);
            }

            long secondsToSunset = now.until(sunset, ChronoUnit.SECONDS);
            Log.message("Timing sunset to " + sunset.toString() + " (" + secondsToSunset + "s remaining)");
            sunsetSchedule = executor.schedule(listener::sunset, secondsToSunset, TimeUnit.SECONDS);
        }

        if (midnightSchedule != null) {
            midnightSchedule.cancel(false);
        }
        ZonedDateTime tomorrow = ZonedDateTime.now();
        tomorrow = tomorrow.plusDays(1).withHour(0).withMinute(1);
        long secondsToMidnight = now.until(tomorrow, ChronoUnit.SECONDS);
        Log.message("Timing rescheduling of sunset/sunrise timers to " + tomorrow.toString() + "(" + secondsToMidnight + "s remaining)");
        midnightSchedule = executor.schedule(this::scheduleEvents, secondsToMidnight, TimeUnit.SECONDS);
    }

    private void refreshTimes() {
        GregorianCalendar now = GregorianCalendar.from(ZonedDateTime.now());
        sunrise = ZonedDateTime.ofInstant(calculator.getOfficialSunriseCalendarForDate(now).toInstant(), ZoneId.systemDefault());
        sunset = ZonedDateTime.ofInstant(calculator.getOfficialSunsetCalendarForDate(now).toInstant(), ZoneId.systemDefault());
    }
}
