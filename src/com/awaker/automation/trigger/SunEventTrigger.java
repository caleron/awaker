package com.awaker.automation.trigger;

import com.awaker.sntp_client.SntpClient;
import com.awaker.util.Log;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SunEventTrigger extends BaseTrigger {

    private final SunriseSunsetCalculator calculator;
    private final SunEvent type;
    private ZonedDateTime sunrise;
    private ZonedDateTime sunset;
    private final int offset;

    public SunEventTrigger(SunEvent type, int offset) {
        this.type = type;
        this.offset = offset;
        Location location = new Location("53.4842682", "10.1460763");
        calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
    }

    @Override
    public List<ScheduledFuture> scheduleForToday(ZonedDateTime now, Runnable runnable, ScheduledExecutorService executorService) {
        List<ScheduledFuture> list = new ArrayList<>();

        GregorianCalendar calendar = GregorianCalendar.from(now);

        if (type == SunEvent.SUNRISE) {
            sunrise = ZonedDateTime.ofInstant(calculator.getOfficialSunriseCalendarForDate(calendar).toInstant(), ZoneId.systemDefault());

            if (now.isBefore(sunrise)) {
                ZonedDateTime eventTime = sunrise.plusSeconds(offset);
                long secondsToSunrise = now.until(eventTime, ChronoUnit.SECONDS);

                Log.message("Timing sunrise to " + eventTime.toString() + " (" + secondsToSunrise + "s remaining)");
                list.add(executorService.schedule(runnable, secondsToSunrise, TimeUnit.SECONDS));
            }
        } else if (type == SunEvent.SUNSET) {

            sunset = ZonedDateTime.ofInstant(calculator.getOfficialSunsetCalendarForDate(calendar).toInstant(), ZoneId.systemDefault());
            if (now.isBefore(sunset)) {
                ZonedDateTime eventTime = sunset.plusSeconds(offset);
                long secondsToSunset = now.until(eventTime, ChronoUnit.SECONDS);

                Log.message("Timing sunset to " + eventTime.toString() + " (" + secondsToSunset + "s remaining)");
                list.add(executorService.schedule(runnable, secondsToSunset, TimeUnit.SECONDS));
            }
        }

        return list;
    }

    @Override
    public void unregisterEvents() {

    }

    public enum SunEvent {
        SUNRISE,
        SUNSET
    }
}
