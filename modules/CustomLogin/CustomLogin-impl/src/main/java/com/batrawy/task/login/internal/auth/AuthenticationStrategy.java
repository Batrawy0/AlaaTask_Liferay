package com.batrawy.task.login.internal.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.model.User;

/**
 * Strategy interface for different authentication mechanisms
 */
public interface AuthenticationStrategy {

    /**
     * Authenticates a user based on the login request
     *
     * @param loginRequest The login request containing credentials
     * @param user The user to authenticate
     * @param loginResponse The response object to populate
     * @return true if authentication is successful, false otherwise
     */
    boolean authenticate(LoginRequest loginRequest, User user, LoginResponse loginResponse);
}