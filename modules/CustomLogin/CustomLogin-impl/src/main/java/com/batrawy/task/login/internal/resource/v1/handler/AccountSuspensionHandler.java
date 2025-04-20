package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.service.SuspensionService;

/**
 * Checks if the user account is suspended.
 */
public class AccountSuspensionHandler extends BaseLoginHandler {

    private final SuspensionService suspensionService;

    public AccountSuspensionHandler(SuspensionService suspensionService) {
        this.suspensionService = suspensionService;
    }

    @Override
    public boolean handle(LoginRequest request, LoginResponse response, LoginContext context) {
        String email = context.getEmail();

        if (suspensionService.isUserSuspended(email)) {
            response.setStatusCode(403);
            response.setStatusMessage("Account temporarily suspended due to too many failed login attempts. Please try again later.");
            return false;
        }

        return continueWithNext(request, response, context);
    }
}