package com.fivevision.api.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;

    private static final Map<String, RateLimitConfig> LIMITS = Map.of(
            "/api/v1/media/upload-url", new RateLimitConfig(10, Duration.ofMinutes(1)),
            "/api/v1/users/sync",      new RateLimitConfig(5, Duration.ofMinutes(1)),
            "/api/v1/cards",            new RateLimitConfig(20, Duration.ofMinutes(1))
    );

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitConfig config = LIMITS.get(path);

        if (config == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            userId = auth.getName();
        }

        String key = userId + ":" + path;
        log.info("Rate limit check: path={}, userId={}, key={}", path, userId, key);

        if (rateLimitService.tryConsume(key, config.capacity, config.refillDuration)) {
            log.info("Allowed for key={}", key);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key={}", key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }

    private record RateLimitConfig(int capacity, Duration refillDuration) {}
}