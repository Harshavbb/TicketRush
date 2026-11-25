package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final long WINDOW_MILLIS = 10_000L; // 5 hold requests per 10 seconds per IP

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();
        String key = "ratelimit:hold:" + clientIp;

        try {
            Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(WINDOW_MILLIS)
            );

            if (count != null && count > MAX_REQUESTS_PER_WINDOW) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "10");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many booking attempts. Try again in 10 seconds.\"}");
                return false;
            }
        } catch (Exception ignored) {
            // Fail open if Redis is unavailable
        }

        return true;
    }
}