package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.service.FailedAttemptsService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;

/**
 * Looks up the user by email address.
 */
public class UserLookupHandler extends BaseLoginHandler {

    private static final Log _log = LogFactoryUtil.getLog(UserLookupHandler.class);

    private final UserLocalService userLocalService;
    private final FailedAttemptsService failedAttemptsService;

    public UserLookupHandler(UserLocalService userLocalService,
                             FailedAttemptsService failedAttemptsService) {
        this.userLocalService = userLocalService;
        this.failedAttemptsService = failedAttemptsService;
    }

    @Override
    public boolean handle(LoginRequest request, LoginResponse response, LoginContext context) {
        String email = context.getEmail();

        // Retrieve the user by email address
        User user;
        try {
            user = userLocalService.getUserByEmailAddress(context.getCompanyId(), email);
            context.setUser(user);
        } catch (PortalException e) {
            _log.debug("User not found with email: " + email, e);

            // Increment failed attempts for non-existent users too (to prevent user enumeration)
            failedAttemptsService.incrementFailedAttempts(email);

            response.setStatusCode(400);
            response.setStatusMessage("Invalid credentials.");
            return false;
        }

        return continueWithNext(request, response, context);
    }
}