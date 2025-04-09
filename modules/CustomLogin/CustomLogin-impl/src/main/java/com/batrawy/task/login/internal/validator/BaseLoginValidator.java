package com.batrawy.task.login.internal.validator;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;

/**
 * Base implementation of the LoginValidator interface
 */
public abstract class BaseLoginValidator implements LoginValidator {

    protected LoginValidator nextValidator;

    @Override
    public LoginValidator setNext(LoginValidator nextValidator) {
        this.nextValidator = nextValidator;
        return nextValidator;
    }

    /**
     * Calls the next validator in the chain if it exists
     *
     * @param loginRequest The login request to validate
     * @param loginResponse The response to populate if validation fails
     * @return true if validation passes, false otherwise
     */
    protected boolean checkNext(LoginRequest loginRequest, LoginResponse loginResponse) {
        if (nextValidator == null) {
            return true;
        }
        return nextValidator.validate(loginRequest, loginResponse);
    }
}