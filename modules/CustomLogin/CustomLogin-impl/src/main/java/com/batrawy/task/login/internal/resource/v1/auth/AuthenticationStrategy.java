package com.batrawy.task.login.internal.resource.v1.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.handler.LoginContext;

/**
 * Strategy interface for different authentication methods.
 */
public interface AuthenticationStrategy {

    /**
     * Authenticate a user based on the request.
     *
     * @param request The login request
     * @param response The login response to be populated
     * @param context The login context with shared data
     * @return true if authentication is successful, false otherwise
     */
    boolean authenticate(LoginRequest request, LoginResponse response, LoginContext context);
}