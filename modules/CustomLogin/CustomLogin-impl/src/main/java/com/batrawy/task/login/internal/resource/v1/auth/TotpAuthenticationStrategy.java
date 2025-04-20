package com.batrawy.task.login.internal.resource.v1.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.handler.LoginContext;
import com.batrawy.task.login.internal.resource.v1.service.FailedAttemptsService;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.Validator;
import com.warrenstrange.googleauth.GoogleAuthenticator;

/**
 * TOTP-based two-factor authentication strategy.
 */
public class TotpAuthenticationStrategy implements AuthenticationStrategy {

    private static final Log _log = LogFactoryUtil.getLog(TotpAuthenticationStrategy.class);
    private final FailedAttemptsService failedAttemptsService;

    public TotpAuthenticationStrategy(FailedAttemptsService failedAttemptsService) {
        this.failedAttemptsService = failedAttemptsService;
    }

    @Override
    public boolean authenticate(LoginRequest request, LoginResponse response, LoginContext context) {
        String email = context.getEmail();
        User user = context.getUser();

        // Retrieve stored TOTP secret from the Expando field
        ExpandoValue totpSecretValue;
        try {
            totpSecretValue = ExpandoValueLocalServiceUtil.getValue(
                    user.getCompanyId(),
                    User.class.getName(),
                    "CUSTOM_FIELDS",
                    "TOTPSecret",
                    user.getUserId()
            );
        } catch (Exception e) {
            _log.error("Error retrieving TOTP secret for user: " + user.getUserId(), e);
            response.setStatusCode(500);
            response.setStatusMessage("Internal server error while validating 2FA.");
            return false;
        }

        String storedTotpSecret = totpSecretValue != null ? totpSecretValue.getData() : null;
        if (Validator.isNull(storedTotpSecret)) {
            response.setStatusCode(400);
            response.setStatusMessage("User has not enabled 2FA.");
            return false;
        }

        // Verify the TOTP code to be numeric
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        int totpCode;
        try {
            totpCode = Integer.parseInt(request.getTotpCode());
        } catch (NumberFormatException nfe) {
            failedAttemptsService.incrementFailedAttempts(email);
            response.setStatusCode(400);
            response.setStatusMessage("TOTP code must be numeric.");
            return false;
        }

        // Verify the TOTP code
        boolean totpValid = gAuth.authorize(storedTotpSecret, totpCode);
        if (!totpValid) {
            failedAttemptsService.incrementFailedAttempts(email);
            response.setStatusCode(400);
            response.setStatusMessage("Invalid TOTP code.");
            return false;
        }

        return true;
    }
}