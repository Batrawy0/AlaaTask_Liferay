package com.batrawy.task.login.internal.resource.v1;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.factory.ValidatorFactory;
import com.batrawy.task.login.internal.service.AuthenticationService;
import com.batrawy.task.login.internal.validator.LoginValidator;
import com.batrawy.task.login.resource.v1.LoginResource;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * Implementation of the Login Resource API
 * Uses Facade pattern to simplify the login process
 */
@Component(
        properties = "OSGI-INF/liferay/rest/v1/login.properties",
        scope = ServiceScope.PROTOTYPE,
        service = LoginResource.class
)
public class LoginResourceImpl extends BaseLoginResourceImpl {

    private static final Log _log = LogFactoryUtil.getLog(LoginResourceImpl.class);

    @Reference
    private UserLocalService userLocalService;

    @Reference
    private ValidatorFactory validatorFactory;

    @Reference
    private AuthenticationService authenticationService;

    @Override
    public LoginResponse postLogin(LoginRequest loginRequest) throws Exception {
        LoginResponse loginResponse = new LoginResponse();

        try {
            // Create validator chain and validate the request
            LoginValidator validator = validatorFactory.createLoginValidatorChain();
            if (!validator.validate(loginRequest, loginResponse)) {
                return loginResponse;
            }

            // Retrieve the user by email address
            User user;
            try {
                user = userLocalService.getUserByEmailAddress(
                        contextCompany.getCompanyId(), loginRequest.getEmailAddress());
            } catch (Exception e) {
                loginResponse.setStatusCode(400);
                loginResponse.setStatusMessage("Invalid credentials.");
                return loginResponse;
            }

            // Authenticate the user
            if (!authenticationService.authenticate(loginRequest, user, loginResponse)) {
                return loginResponse;
            }

            // At this point, authentication was successful and loginResponse has been populated
            return loginResponse;

        } catch (Exception e) {
            _log.error("Unexpected error during login", e);
            loginResponse.setStatusCode(500);
            loginResponse.setStatusMessage("Internal server error.");
            return loginResponse;
        }
    }
}