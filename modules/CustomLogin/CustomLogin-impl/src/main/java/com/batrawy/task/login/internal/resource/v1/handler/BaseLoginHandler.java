package com.batrawy.task.login.internal.resource.v1.handler;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

/**
 * Base implementation of login handler with chain logic.
 */
public abstract class BaseLoginHandler implements LoginHandler {

    private static final Log _log = LogFactoryUtil.getLog(BaseLoginHandler.class);
    protected LoginHandler nextHandler;

    @Override
    public void setNext(LoginHandler next) {
        this.nextHandler = next;
    }

    /**
     * Continues the chain if there is a next handler
     */
    protected boolean continueWithNext(LoginRequest request, LoginResponse response,
                                       LoginContext loginContext) {
        if (nextHandler != null) {
            return nextHandler.handle(request, response, loginContext);
        }
        return true;
    }
}