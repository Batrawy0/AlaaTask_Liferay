package com.batrawy.task.login.internal.service;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.auth.AuthenticationStrategy;
import com.batrawy.task.login.internal.auth.LiferayBasicAuthStrategy;
import com.batrawy.task.login.internal.auth.TOTPAuthenticationStrategy;
import com.batrawy.task.login.internal.cache.LoginCacheManager;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service for handling authentication
 */
@Component(
        immediate = true,
        service = AuthenticationService.class
)
public class AuthenticationService {

    private static final Log _log = LogFactoryUtil.getLog(AuthenticationService.class);

    @Reference
    private TOTPAuthenticationStrategy totpAuthenticationStrategy;

    @Reference
    private LiferayBasicAuthStrategy liferayBasicAuthStrategy;

    @Reference
    private LoginCacheManager loginCacheManager;

    /**
     * Authenticates a user using multiple authentication strategies
     *
     * @param loginRequest The login request
     * @param user The user to authenticate
     * @param loginResponse The response to populate
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticate(LoginRequest loginRequest, User user, LoginResponse loginResponse) {
        String email = loginRequest.getEmailAddress();

        // First, authenticate with TOTP
        if (!totpAuthenticationStrategy.authenticate(loginRequest, user, loginResponse)) {
            loginCacheManager.incrementFailedAttempts(email);
            return false;
        }

        // Then, authenticate with Liferay Basic Auth
        if (!liferayBasicAuthStrategy.authenticate(loginRequest, user, loginResponse)) {
            loginCacheManager.incrementFailedAttempts(email);
            return false;
        }

        // If all authentication strategies pass, reset failed attempts
        loginCacheManager.resetFailedAttempts(email);
        return true;
    }
}