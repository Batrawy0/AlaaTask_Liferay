package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;

/**
 * Interface for login validation/processing handlers using Chain of Responsibility pattern.
 */
public interface LoginHandler {
    /**
     * Process the login request or pass to the next handler if this handler cannot process it.
     *
     * @param request The login request
     * @param response The login response being built
     * @param loginContext The context containing shared data between handlers
     * @return true if processing should continue, false if chain should stop
     */
    boolean handle(LoginRequest request, LoginResponse response, LoginContext loginContext);

    /**
     * Set the next handler in the chain
     */
    void setNext(LoginHandler next);
}