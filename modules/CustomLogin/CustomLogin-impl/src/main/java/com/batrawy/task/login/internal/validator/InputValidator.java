package com.batrawy.task.login.internal.validator;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.util.Validator;
import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true,
        service = LoginValidator.class
)
public class InputValidator extends BaseLoginValidator {

    @Override
    public boolean validate(LoginRequest loginRequest, LoginResponse loginResponse) {
        if (Validator.isNull(loginRequest.getEmailAddress()) || Validator.isNull(loginRequest.getPassword())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Email and password are required.");
            return false;
        }

        return checkNext(loginRequest, loginResponse);
    }
}