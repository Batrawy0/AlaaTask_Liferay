package com.batrawy.task.login.internal.resource.v1;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.resource.v1.LoginResource;
import com.batrawy.task.login.internal.resource.v1.auth.AuthenticationStrategy;
import com.batrawy.task.login.internal.resource.v1.auth.LiferayApiAuthenticationStrategy;
import com.batrawy.task.login.internal.resource.v1.auth.TotpAuthenticationStrategy;
import com.batrawy.task.login.internal.resource.v1.handler.*;
import com.batrawy.task.login.internal.resource.v1.service.CaptchaService;
import com.batrawy.task.login.internal.resource.v1.service.FailedAttemptsService;
import com.batrawy.task.login.internal.resource.v1.service.SuspensionService;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.io.Serializable;

@Component(
        properties = "OSGI-INF/liferay/rest/v1/login.properties",
        scope = ServiceScope.PROTOTYPE, service = LoginResource.class
)
public class LoginResourceImpl extends BaseLoginResourceImpl {

    private static final Log _log = LogFactoryUtil.getLog(LoginResourceImpl.class);
    private static final String LIFERAY_API_URL = "http://localhost:8080/o/headless-admin-user/v1.0/my-user-account";

    @Reference
    private UserLocalService userLocalService;

    @Reference
    private MultiVMPool multiVMPool;

    @Reference
    private FailedAttemptsService failedAttemptsService;

    @Reference
    private SuspensionService suspensionService;

    @Reference
    private CaptchaService captchaService;

    private LoginHandler loginChain;
    private AuthenticationStrategy totpStrategy;
    private AuthenticationStrategy liferayApiStrategy;

    @Activate
    protected void activate() {
        // Initialize the authentication strategies
        totpStrategy = new TotpAuthenticationStrategy(failedAttemptsService);
        liferayApiStrategy = new LiferayApiAuthenticationStrategy(failedAttemptsService, LIFERAY_API_URL);

        // Set up the chain of responsibility
        PortalCache<String, Serializable> apiRateLimitCache =
                (PortalCache<String, Serializable>)multiVMPool.getPortalCache("API_RATE_LIMIT_CACHE");

        LoginHandler inputValidator = new InputValidationHandler();
        LoginHandler rateLimiter = new RateLimitHandler(apiRateLimitCache);
        LoginHandler suspensionHandler = new AccountSuspensionHandler(suspensionService);
        LoginHandler captchaHandler = new CaptchaHandler(captchaService, failedAttemptsService);
        LoginHandler userLookupHandler = new UserLookupHandler(userLocalService, failedAttemptsService);

        // Chain the handlers together
        inputValidator.setNext(rateLimiter);
        rateLimiter.setNext(suspensionHandler);
        suspensionHandler.setNext(captchaHandler);
        captchaHandler.setNext(userLookupHandler);

        // Set the first handler as the entry point
        loginChain = inputValidator;
    }

    @Override
    public LoginResponse postLogin(LoginRequest loginRequest) throws Exception {
        LoginResponse loginResponse = new LoginResponse();
        LoginContext context = new LoginContext();
        context.setCompanyId(contextCompany.getCompanyId());

        try {
            // Run the validation chain
            boolean shouldContinue = loginChain.handle(loginRequest, loginResponse, context);

            if (!shouldContinue) {
                return loginResponse;
            }

            // If validation passes, authenticate with TOTP
            if (!totpStrategy.authenticate(loginRequest, loginResponse, context)) {
                return loginResponse;
            }

            // If TOTP auth passes, authenticate with Liferay API
            if (!liferayApiStrategy.authenticate(loginRequest, loginResponse, context)) {
                return loginResponse;
            }

            // If everything passes, return success
            loginResponse.setStatusCode(200);
            loginResponse.setStatusMessage("Login successful.");

        } catch (Exception e) {
            _log.error("Unexpected error during login process", e);
            loginResponse.setStatusCode(500);
            loginResponse.setStatusMessage("Internal server error.");
        }

        return loginResponse;
    }
}