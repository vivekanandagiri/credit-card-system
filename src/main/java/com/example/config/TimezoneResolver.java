package com.example.config;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.example.entity.Customer;
import com.example.util.TimezoneContext;

/**
 * Determines the timezone using priority
 * ---------------------------------------
 * 1. Request context (ThreadLocal)
 * 2. Database (user preference)
 * 3. Default configuration
 * This allows the system to work in both API and background jobs.
 */
@Component
public class TimezoneResolver {

	private final TimezoneConfig timezoneConfig;

    public TimezoneResolver(TimezoneConfig timezoneConfig) {
        this.timezoneConfig = timezoneConfig;
    }

    public ZoneId resolve(Customer customer) {

    	// 1. API flow (ThreadLocal)
        ZoneId contextZone = TimezoneContext.getRawZone();
        if (contextZone != null) {
            return contextZone;
        }


        // 2. Scheduler flow (DB)
        if (customer != null && customer.getTimezone() != null) {
            try {
                return ZoneId.of(customer.getTimezone());
            } catch (Exception ignored) {
            }
        }

        // 3. fallback
        return timezoneConfig.getDefaultZone();
    }
}