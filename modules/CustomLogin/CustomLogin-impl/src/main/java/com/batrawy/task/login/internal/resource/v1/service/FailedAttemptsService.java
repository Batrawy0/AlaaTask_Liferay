package com.batrawy.task.login.internal.resource.v1.service;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.Serializable;

/**
 * Service for managing failed login attempts.
 */
@Component(service = FailedAttemptsService.class)
public class FailedAttemptsService {

    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int SUSPENSION_THRESHOLD = 5;
    private static final int FAILED_ATTEMPTS_TTL = 900; // 15 minutes

    private static final String FAILED_ATTEMPTS_CACHE_PREFIX = "failed_login_attempts_";

    private PortalCache<String, Serializable> failedAttemptsCache;

    @Reference
    private MultiVMPool multiVMPool;

    @Reference
    private SuspensionService suspensionService;

    @Activate
    protected void activate() {
        failedAttemptsCache = (PortalCache<String, Serializable>)
                multiVMPool.getPortalCache("FAILED_ATTEMPTS_CACHE");
    }

    public int getFailedAttempts(String email) {
        Integer attempts = (Integer) failedAttemptsCache.get(FAILED_ATTEMPTS_CACHE_PREFIX + email);
        return attempts != null ? attempts : 0;
    }

    public void incrementFailedAttempts(String email) {
        int attempts = getFailedAttempts(email) + 1;
        failedAttemptsCache.put(FAILED_ATTEMPTS_CACHE_PREFIX + email, attempts, FAILED_ATTEMPTS_TTL);

        if (attempts >= SUSPENSION_THRESHOLD) {
            suspensionService.suspendUser(email);
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