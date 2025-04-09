package com.batrawy.task.login.internal.validator;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;

/**
 * Interface for the Chain of Responsibility pattern for login validation
 */
public interface LoginValidator {

    /**
     * Sets the next validator in the chain
     *
     * @param nextValidator The next validator
     * @return The next validator
     */
    LoginValidator setNext(LoginValidator nextValidator);

    /**
     * Validates the login request
     *
     * @param loginRequest The login request to validate
     * @param loginResponse The response to populate if validation fails
     * @return true if validation passes, false otherwise
     */
    boolean validate(LoginRequest loginRequest, LoginResponse loginResponse);
}