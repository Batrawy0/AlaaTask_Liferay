package com.batrawy.task.login.internal.validator;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.cache.LoginCacheManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = LoginValidator.class
)
public class RateLimitValidator extends BaseLoginValidator {

    @Reference
    private LoginCacheManager loginCacheManager;

    @Override
    public boolean validate(LoginRequest loginRequest, LoginResponse loginResponse) {
        // Check overall API rate limit
        if (loginCacheManager.isOverallRateLimitExceeded()) {
            loginResponse.setStatusCode(429);
            loginResponse.setStatusMessage("Too many requests. Please try again later.");
            return false;
        }

        return checkNext(loginRequest, loginResponse);
    }
}