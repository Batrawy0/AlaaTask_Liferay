package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.cache.PortalCache;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles API rate limiting.
 */
public class RateLimitHandler extends BaseLoginHandler {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int RATE_LIMIT_TTL = 60;
    private static final String API_RATE_LIMIT_CACHE_KEY = "login_api_rate_limit";

    private final PortalCache<String, Serializable> apiRateLimitCache;

    public RateLimitHandler(PortalCache<String, Serializable> apiRateLimitCache) {
        this.apiRateLimitCache = apiRateLimitCache;
    }

    @Override
    public boolean handle(LoginRequest request, LoginResponse response, LoginContext context) {
        if (isOverallRateLimitExceeded()) {
            response.setStatusCode(429);
            response.setStatusMessage("Too many requests. Please try again later.");
            return false;
        }

        return continueWithNext(request, response, context);
    }

    private boolean isOverallRateLimitExceeded() {
        AtomicInteger requestCount = (AtomicInteger) apiRateLimitCache.get(API_RATE_LIMIT_CACHE_KEY);

        if (requestCount == null) {
            requestCount = new AtomicInteger(1);
            apiRateLimitCache.put(API_RATE_LIMIT_CACHE_KEY, requestCount, RATE_LIMIT_TTL);
            return false;
        }

        int currentCount = requestCount.incrementAndGet();
        apiRateLimitCache.put(API_RATE_LIMIT_CACHE_KEY, requestCount, RATE_LIMIT_TTL);

        return currentCount > MAX_REQUESTS_PER_MINUTE;
    }
}