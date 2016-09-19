package com.awaker.automation;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoLighter {
    private final EnvironmentEventListener listener;
    private SunriseSunsetCalculator calculator;
    private Calendar sunrise;
    private Calendar sunset;

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
        Calendar now = Calendar.getInstance();
        if (now.before(sunrise)) {
            long msToSunrise = sunrise.getTimeInMillis() - now.getTimeInMillis();
            if (sunriseSchedule != null) {
                sunriseSchedule.cancel(false);
            }
            sunriseSchedule = executor.schedule(listener::sunrise, msToSunrise, TimeUnit.MILLISECONDS);
        }

        if (now.before(sunset)) {
            long msToSunset = sunset.getTimeInMillis() - now.getTimeInMillis();
            if (sunsetSchedule != null) {
                sunsetSchedule.cancel(false);
            }
            sunsetSchedule = executor.schedule(listener::sunset, msToSunset, TimeUnit.MILLISECONDS);
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 1);
        long msToMidnight = tomorrow.getTimeInMillis() - now.getTimeInMillis();
        if (midnightSchedule != null) {
            midnightSchedule.cancel(false);
        }
        midnightSchedule = executor.schedule(this::scheduleEvents, msToMidnight, TimeUnit.MILLISECONDS);
    }

    private void refreshTimes() {
        Calendar now = Calendar.getInstance();
        sunrise = calculator.getOfficialSunriseCalendarForDate(now);
        sunset = calculator.getOfficialSunsetCalendarForDate(now);
    }
}
