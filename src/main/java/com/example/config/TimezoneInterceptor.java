package com.example.config;

import com.example.util.TimezoneContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;

@Component
public class TimezoneInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String timezone = request.getHeader("X-Timezone");

        try {
            if (timezone != null && !timezone.isEmpty()) {
                TimezoneContext.setZone(ZoneId.of(timezone));
            } else {
                TimezoneContext.setZone(ZoneId.systemDefault());
            }
        } catch (Exception e) {
            TimezoneContext.setZone(ZoneId.systemDefault());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TimezoneContext.clear();
    }
}