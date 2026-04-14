package com.example.util;

import java.time.ZoneId;

/**
 * Utility class for managing user-specific timezone context per request.
 *
 * <p><b>Flow:</b>
 * <pre>
 * Frontend → sends timezone (header)
 *        ↓
 * Interceptor → reads timezone
 *        ↓
 * TimezoneContext (ThreadLocal storage)
 *        ↓
 * Jackson Serializer → converts Instant to user timezone
 *        ↓
 * Response returned in user's timezone
 * </pre>
 *
 * <p><b>Purpose:</b>
 * <ul>
 *     <li>Store user timezone per request thread</li>
 *     <li>Enable timezone-aware serialization/deserialization</li>
 *     <li>Avoid passing timezone across layers explicitly</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * Uses {@link ThreadLocal} to ensure each request has its own isolated timezone.
 *
 * <p><b>Important:</b>
 * Always call {@link #clear()} after request completion
 * to prevent memory leaks in thread pools.
 */
public class TimezoneContext {
    /**
     * Thread-local storage for user timezone.
     */
    private static final ThreadLocal<ZoneId> USER_ZONE = new ThreadLocal<>();

    /**
     * Sets the timezone for the current request thread.
     *
     * @param zoneId user's timezone (e.g., Asia/Kolkata)
     */
    public static void setZone(ZoneId zoneId) {
        USER_ZONE.set(zoneId);
    }
    /**
     * Returns the raw timezone stored in the current thread.
     *
     * <p>May return null if not set.
     *
     * @return ZoneId or null
     */
    public static ZoneId getRawZone() {
        return USER_ZONE.get();
    }
    /**
     * Clears the timezone from the current thread.
     *
     * <p><b>Must be called after request completion</b>
     * (e.g., in a filter or interceptor) to prevent memory leaks.
     */
    public static void clear() {
        USER_ZONE.remove();
    }
}