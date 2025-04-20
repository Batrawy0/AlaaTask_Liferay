package com.batrawy.task.login.internal.resource.v1.service;

import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.Serializable;

/**
 * Service for managing user suspensions.
 */
@Component(service = SuspensionService.class)
public class SuspensionService {

    private static final int SUSPENSION_TTL = 60; // 1 minute
    private static final String SUSPENSION_CACHE_PREFIX = "user_suspension_";

    private PortalCache<String, Serializable> suspensionCache;

    @Reference
    private MultiVMPool multiVMPool;

    @Activate
    protected void activate() {
        suspensionCache = (PortalCache<String, Serializable>)
                multiVMPool.getPortalCache("SUSPENSION_CACHE");
    }

    public boolean isUserSuspended(String email) {
        Boolean suspended = (Boolean) suspensionCache.get(SUSPENSION_CACHE_PREFIX + email);
        return suspended != null && suspended;
    }

    public void suspendUser(String email) {
        suspensionCache.put(SUSPENSION_CACHE_PREFIX + email, true, SUSPENSION_TTL);
    }

    public void removeSuspension(String email) {
        suspensionCache.remove(SUSPENSION_CACHE_PREFIX + email);
    }
}