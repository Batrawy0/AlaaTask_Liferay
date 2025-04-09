package com.batrawy.task.login.internal.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.Validator;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true,
        service = AuthenticationStrategy.class
)
public class TOTPAuthenticationStrategy implements AuthenticationStrategy {

    private static final Log _log = LogFactoryUtil.getLog(TOTPAuthenticationStrategy.class);

    @Override
    public boolean authenticate(LoginRequest loginRequest, User user, LoginResponse loginResponse) {
        try {
            // Retrieve stored TOTP secret from the Expando field
            ExpandoValue totpSecret = ExpandoValueLocalServiceUtil.getValue(
                    user.getCompanyId(),
                    User.class.getName(),
                    "CUSTOM_FIELDS",
                    "TOTPSecret",
                    user.getUserId()
            );

            String storedTotpSecret = totpSecret.getData();
            if (Validator.isNull(storedTotpSecret)) {
                loginResponse.setStatusCode(400);
                loginResponse.setStatusMessage("User has not enabled 2FA.");
                return false;
            }

            // Verify the TOTP code to be numeric
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            int totpCode;
            try {
                totpCode = Integer.parseInt(loginRequest.getTotpCode());
            } catch (NumberFormatException nfe) {
                loginResponse.setStatusCode(400);
                loginResponse.setStatusMessage("TOTP code must be numeric.");
                return false;
            }

            // Verify the TOTP code
            boolean totpValid = gAuth.authorize(storedTotpSecret, totpCode);
            if (!totpValid) {
                loginResponse.setStatusCode(400);
                loginResponse.setStatusMessage("Invalid TOTP code.");
                return false;
            }

            return true;
        } catch (Exception e) {
            _log.error("Error during TOTP authentication", e);
            loginResponse.setStatusCode(500);
            loginResponse.setStatusMessage("Internal server error during authentication.");
            return false;
        }
    }
}