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
    private final SunriseSunsetCalculator calculator;
    private ZonedDateTime sunrise;
    private ZonedDateTime sunset;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> sunriseSchedule, sunsetSchedule, midnightSchedule;

    private AutoLighter(EnvironmentEventListener listener) {
        this.listener = listener;
        Location location = new Location("53.4842682", "10.1460763");
        calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());

        if (Config.getBool(ConfigKey.LIGHT_ON_SUNRISE) || Config.getBool(ConfigKey.LIGHT_ON_SUNSET)) {
            scheduleEvents();
        }

        ConfigKey[] listenEvents = new ConfigKey[]{
                ConfigKey.SUNRISE_TIME_OFFSET_SECONDS, ConfigKey.SUNSET_TIME_OFFSET_SECONDS,
                ConfigKey.LIGHT_ON_SUNRISE, ConfigKey.LIGHT_ON_SUNSET
        };
        Config.addListener(this, listenEvents);
    }

    public static void start(EnvironmentEventListener listener) {
        new Thread(() -> new AutoLighter(listener)).start();
    }
//TODO Sommer/Winterzeit berücksichtigen
    private void scheduleEvents() {
        refreshTimes();
        ZonedDateTime now = null;
        while (now == null) {
            now = SntpClient.getTime();
            if (now == null) {
                Log.message("Getting Time failed, retrying in 30 seconds");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    Log.error(e);
                }
            }
        }

        int sunriseOffset = Config.getInt(ConfigKey.SUNRISE_TIME_OFFSET_SECONDS);
        int sunsetOffset = Config.getInt(ConfigKey.SUNSET_TIME_OFFSET_SECONDS);

        //Events abbrechen, falls gesetzt
        if (sunriseSchedule != null) {
            sunriseSchedule.cancel(false);
        }
        if (sunsetSchedule != null) {
            sunsetSchedule.cancel(false);
        }
        if (midnightSchedule != null) {
            midnightSchedule.cancel(false);
        }

        //Sonnenaufgang setzen
        if (now.isBefore(sunrise) && Config.getBool(ConfigKey.LIGHT_ON_SUNRISE)) {
            ZonedDateTime lightTime = sunrise.plusSeconds(sunriseOffset);
            long secondsToSunrise = now.until(lightTime, ChronoUnit.SECONDS);

            Log.message("Timing sunrise to " + lightTime.toString() + " (" + secondsToSunrise + "s remaining)");
            sunriseSchedule = executor.schedule(listener::sunrise, secondsToSunrise, TimeUnit.SECONDS);
        }

        //Sonnenuntergang setzen
        if (now.isBefore(sunset) && Config.getBool(ConfigKey.LIGHT_ON_SUNSET)) {
            ZonedDateTime lightTime = sunset.plusSeconds(sunsetOffset);
            long secondsToSunset = now.until(lightTime, ChronoUnit.SECONDS);

            Log.message("Timing sunset to " + lightTime.toString() + " (" + secondsToSunset + "s remaining)");
            sunsetSchedule = executor.schedule(listener::sunset, secondsToSunset, TimeUnit.SECONDS);
        }

        //Um 00:01 alle Timer neu setzen
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
