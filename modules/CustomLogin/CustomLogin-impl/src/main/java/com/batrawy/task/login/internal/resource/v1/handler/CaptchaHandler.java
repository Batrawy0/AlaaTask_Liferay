package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.service.CaptchaService;
import com.batrawy.task.login.internal.resource.v1.service.FailedAttemptsService;
import com.liferay.portal.kernel.util.Validator;

/**
 * Handles CAPTCHA verification.
 */
public class CaptchaHandler extends BaseLoginHandler {

    private final CaptchaService captchaService;
    private final FailedAttemptsService failedAttemptsService;

    public CaptchaHandler(CaptchaService captchaService, FailedAttemptsService failedAttemptsService) {
        this.captchaService = captchaService;
        this.failedAttemptsService = failedAttemptsService;
    }

    @Override
    public boolean handle(LoginRequest request, LoginResponse response, LoginContext context) {
        String email = context.getEmail();

        // Check if CAPTCHA is required but not provided
        if (failedAttemptsService.requiresCaptcha(email) && Validator.isNull(request.getCaptchaResponse())) {
            response.setStatusCode(400);
            response.setStatusMessage("CAPTCHA verification required.");
            response.setRequireCaptcha(true);
            return false;
        }

        // If CAPTCHA is required, verify it
        if (failedAttemptsService.requiresCaptcha(email) &&
                !captchaService.verifyCaptcha(request.getCaptchaResponse())) {
            response.setStatusCode(400);
            response.setStatusMessage("Invalid CAPTCHA response.");
            response.setRequireCaptcha(true);
            return false;
        }

        return continueWithNext(request, response, context);
    }
}