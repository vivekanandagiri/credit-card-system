package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class TimezoneConfig {

    public static ZoneId DEFAULT_ZONE;

    public TimezoneConfig(@Value("${app.default-timezone}") String timezone) {
        DEFAULT_ZONE = ZoneId.of(timezone);
    }
}