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
public class AccountStatusValidator extends BaseLoginValidator {

    @Reference
    private LoginCacheManager loginCacheManager;

    @Override
    public boolean validate(LoginRequest loginRequest, LoginResponse loginResponse) {
        String email = loginRequest.getEmailAddress();

        // Check if user is suspended
        if (loginCacheManager.isUserSuspended(email)) {
            loginResponse.setStatusCode(403);
            loginResponse.setStatusMessage("Account temporarily suspended due to too many failed login attempts. Please try again later.");
            return false;
        }

        return checkNext(loginRequest, loginResponse);
    }
}