package com.example.util;

import java.time.ZoneId;

import com.example.config.TimezoneConfig;

/**
 * Frontend → sends timezone (header)
        ↓
   Intercepter → reads timezone
        ↓
   TimezoneContext (ThreadLocal)
        ↓
   Jackson Serializer → converts Instant
        ↓
   Response (user timezone)
 * 
 */

public class TimezoneContext {

    private static final ThreadLocal<ZoneId> USER_ZONE = new ThreadLocal<>();

    public static void setZone(ZoneId zoneId) {
        USER_ZONE.set(zoneId);
    }

    public static ZoneId getZone() {
        return USER_ZONE.get() != null
                ? USER_ZONE.get()
                : TimezoneConfig.DEFAULT_ZONE; //Default zone
    }

    public static void clear() {
        USER_ZONE.remove();
    }
}