package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.util.Validator;

/**
 * Validates the input parameters of the login request.
 */
public class InputValidationHandler extends BaseLoginHandler {

    @Override
    public boolean handle(LoginRequest request, LoginResponse response, LoginContext context) {
        if (Validator.isNull(request.getEmailAddress()) || Validator.isNull(request.getPassword())) {
            response.setStatusCode(400);
            response.setStatusMessage("Email and password are required.");
            return false;
        }

        context.setEmail(request.getEmailAddress());
        return continueWithNext(request, response, context);
    }
}