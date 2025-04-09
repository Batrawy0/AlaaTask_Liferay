package com.batrawy.task.login.internal.cache;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton cache manager for login-related caching
 */
@Component(
        immediate = true,
        service = LoginCacheManager.class
)
public class LoginCacheManager {

    private static final Log _log = LogFactoryUtil.getLog(LoginCacheManager.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int SUSPENSION_THRESHOLD = 5;

    private static final int RATE_LIMIT_TTL = 60;
    private static final int FAILED_ATTEMPTS_TTL = 900;
    private static final int SUSPENSION_TTL = 900;

    private static final String API_RATE_LIMIT_CACHE_KEY = "login_api_rate_limit";
    private static final String FAILED_ATTEMPTS_CACHE_PREFIX = "failed_login_attempts_";
    private static final String SUSPENSION_CACHE_PREFIX = "user_suspension_";

    private PortalCache<String, Serializable> apiRateLimitCache;
    private PortalCache<String, Serializable> failedAttemptsCache;
    private PortalCache<String, Serializable> suspensionCache;

    @Reference
    private MultiVMPool multiVMPool;

    @Activate
    protected void activate() {
        apiRateLimitCache = (PortalCache<String, Serializable>) multiVMPool.getPortalCache("API_RATE_LIMIT_CACHE");
        failedAttemptsCache = (PortalCache<String, Serializable>) multiVMPool.getPortalCache("FAILED_ATTEMPTS_CACHE");
        suspensionCache = (PortalCache<String, Serializable>) multiVMPool.getPortalCache("SUSPENSION_CACHE");
    }

    public boolean isOverallRateLimitExceeded() {
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

    public boolean isUserSuspended(String email) {
        Boolean suspended = (Boolean) suspensionCache.get(SUSPENSION_CACHE_PREFIX + email);
        return suspended != null && suspended;
    }

    public int getFailedAttempts(String email) {
        Integer attempts = (Integer) failedAttemptsCache.get(FAILED_ATTEMPTS_CACHE_PREFIX + email);
        return attempts != null ? attempts : 0;
    }

    public void incrementFailedAttempts(String email) {
        int attempts = getFailedAttempts(email) + 1;
        failedAttemptsCache.put(FAILED_ATTEMPTS_CACHE_PREFIX + email, attempts, FAILED_ATTEMPTS_TTL);

        if (attempts >= SUSPENSION_THRESHOLD) {
            suspensionCache.put(SUSPENSION_CACHE_PREFIX + email, true, SUSPENSION_TTL);
        }
    }

    public void resetFailedAttempts(String email) {
        failedAttemptsCache.remove(FAILED_ATTEMPTS_CACHE_PREFIX + email);
    }

    public boolean requiresCaptcha(String email) {
        int attempts = getFailedAttempts(email);
        return attempts >= CAPTCHA_THRESHOLD;
    }
}