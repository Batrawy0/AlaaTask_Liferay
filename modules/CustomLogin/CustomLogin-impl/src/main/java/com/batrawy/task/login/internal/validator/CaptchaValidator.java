package com.batrawy.task.login.internal.validator;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.cache.LoginCacheManager;
import com.batrawy.task.login.internal.service.RecaptchaService;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        immediate = true,
        service = LoginValidator.class
)
public class CaptchaValidator extends BaseLoginValidator {

    @Reference
    private LoginCacheManager loginCacheManager;

    @Reference
    private RecaptchaService recaptchaService;

    @Override
    public boolean validate(LoginRequest loginRequest, LoginResponse loginResponse) {
        String email = loginRequest.getEmailAddress();

        // Check if CAPTCHA is required but not provided
        if (loginCacheManager.requiresCaptcha(email) && Validator.isNull(loginRequest.getCaptchaResponse())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("CAPTCHA verification required.");
            loginResponse.setRequireCaptcha(true);
            return false;
        }

        // If CAPTCHA is required, verify it
        if (loginCacheManager.requiresCaptcha(email) && !recaptchaService.verifyRecaptcha(loginRequest.getCaptchaResponse(), null)) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Invalid CAPTCHA response.");
            loginResponse.setRequireCaptcha(true);
            return false;
        }

        return checkNext(loginRequest, loginResponse);
    }
}