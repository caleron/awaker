package com.awaker.automation;

import com.awaker.config.Config;
import com.awaker.config.ConfigChangeListener;
import com.awaker.config.ConfigKey;
import com.awaker.sntp_client.SntpClient;
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

class AutoLighter implements ConfigChangeListener {
    private final EnvironmentEventListener listener;
    private SunriseSunsetCalculator calculator;
    private ZonedDateTime sunrise;
    private ZonedDateTime sunset;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> sunriseSchedule, sunsetSchedule, midnightSchedule;

    private AutoLighter(EnvironmentEventListener listener) {
        this.listener = listener;
        Location location = new Location("53.4842682", "10.1460763");
        calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
        scheduleEvents();

        ConfigKey[] listenEvents = new ConfigKey[]{
                ConfigKey.SUNRISE_TIME_OFFSET, ConfigKey.SUNSET_TIME_OFFSET,
                ConfigKey.LIGHT_ON_SUNRISE, ConfigKey.LIGHT_ON_SUNSET
        };
        Config.addListener(this, listenEvents);
    }

    public static void start(EnvironmentEventListener listener) {
        new Thread(() -> new AutoLighter(listener)).start();
    }

    private void scheduleEvents() {
        refreshTimes();
        ZonedDateTime now = null;
        while (now == null) {
            now = SntpClient.getTime();
            if (now == null) {
                Log.message("Getting Time failed, retrying in 20 seconds");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Log.error(e);
                }
            }
        }

        int sunriseOffset = Config.getInt(ConfigKey.SUNRISE_TIME_OFFSET, 0);
        int sunsetOffset = Config.getInt(ConfigKey.SUNSET_TIME_OFFSET, 0);

        if (now.isBefore(sunrise)) {
            if (sunriseSchedule != null) {
                sunriseSchedule.cancel(false);
            }

            long secondsToSunrise = now.until(sunrise, ChronoUnit.SECONDS);
            secondsToSunrise += sunriseOffset;

            Log.message("Timing sunrise to " + sunrise.toString() + " (" + secondsToSunrise + "s remaining)");
            sunriseSchedule = executor.schedule(listener::sunrise, secondsToSunrise, TimeUnit.SECONDS);
        }

        if (now.isBefore(sunset)) {
            if (sunsetSchedule != null) {
                sunsetSchedule.cancel(false);
            }

            long secondsToSunset = now.until(sunset, ChronoUnit.SECONDS);
            secondsToSunset += sunsetOffset;

            Log.message("Timing sunset to " + sunset.toString() + " (" + secondsToSunset + "s remaining)");
            sunsetSchedule = executor.schedule(listener::sunset, secondsToSunset, TimeUnit.SECONDS);
        }

        if (midnightSchedule != null) {
            midnightSchedule.cancel(false);
        }

        ZonedDateTime tomorrow = now.plusDays(1).withHour(0).withMinute(1);
        long secondsToMidnight = now.until(tomorrow, ChronoUnit.SECONDS);
        Log.message("Timing rescheduling of sunset/sunrise timers to " + tomorrow.toString() + "(" + secondsToMidnight + "s remaining)");
        midnightSchedule = executor.schedule(this::scheduleEvents, secondsToMidnight, TimeUnit.SECONDS);
    }

    private void refreshTimes() {
        GregorianCalendar now = GregorianCalendar.from(ZonedDateTime.now());
        sunrise = ZonedDateTime.ofInstant(calculator.getOfficialSunriseCalendarForDate(now).toInstant(), ZoneId.systemDefault());
        sunset = ZonedDateTime.ofInstant(calculator.getOfficialSunsetCalendarForDate(now).toInstant(), ZoneId.systemDefault());
    }

    @Override
    public void configChanged(ConfigKey key) {
        scheduleEvents();
    }
}
