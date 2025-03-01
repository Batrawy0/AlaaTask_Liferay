package com.batrawy.task.login.internal.resource.v1;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.resource.v1.LoginResource;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Validator;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
        properties = "OSGI-INF/liferay/rest/v1/login.properties",
        scope = ServiceScope.PROTOTYPE, service = LoginResource.class
)
public class LoginResourceImpl extends BaseLoginResourceImpl {

    @Reference
    private UserLocalService userLocalService;

    @Override
    public LoginResponse postLogin(LoginRequest loginRequest) throws Exception {
        LoginResponse loginResponse = new LoginResponse();

        // Validate the input
        if (Validator.isNull(loginRequest.getEmailAddress()) || Validator.isNull(loginRequest.getPassword())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Email and password are required.");
            return loginResponse;
        }

        // Retrieve the user by email address
        User user;
        try {
            user = userLocalService.getUserByEmailAddress(contextCompany.getCompanyId(), loginRequest.getEmailAddress());
        } catch (Exception e) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Invalid credentials.");
            return loginResponse;
        }

        // Retrieve stored TOTP secret from the Expando field
//        String storedTotpSecret = (String) user.getExpandoBridge().getAttribute("TOTPSecret");
        ExpandoValue totpSecret = ExpandoValueLocalServiceUtil.getValue(
                user.getCompanyId(),
                User.class.getName(),
                "CUSTOM_FIELDS",
                "TOTPSecret",
                user.getUserId()
        );
        String storedTotpSecret = totpSecret.getData();
        if (Validator.isNull(storedTotpSecret)) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("User has not enabled 2FA.");
            return loginResponse;
        }

        // Verify the TOTP code to be numeric
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        int totpCode;
        try {
            totpCode = Integer.parseInt(loginRequest.getTotpCode());
        } catch (NumberFormatException nfe) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("TOTP code must be numeric.");
            return loginResponse;
        }

        // Verify the TOTP code
        boolean totpValid = gAuth.authorize(storedTotpSecret, totpCode);
        if (!totpValid) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Invalid TOTP code.");
            return loginResponse;
        }


        String builtInLoginUrl = "http://localhost:8080/o/headless-admin-user/v1.0/my-user-account";

        // Create HttpClient instance
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create the GET request
            HttpGet httpGet = new HttpGet(builtInLoginUrl);

            // Add Basic Authentication header
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(loginRequest.getEmailAddress(), loginRequest.getPassword());
            httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet, null));

            // Execute the request
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(httpResponse.getEntity());

            if (statusCode != 200) {
                loginResponse.setStatusCode(statusCode);
                loginResponse.setStatusMessage("Authentication failed via built-in API.");
                return loginResponse;
            }

            // Parse the responseBody
            JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(responseBody);
            System.out.println(jsonResponse);
            loginResponse.setUserId(jsonResponse.getLong("id"));
            loginResponse.setScreenName(jsonResponse.getString("name"));
            loginResponse.setStatusCode(200);
            loginResponse.setStatusMessage("Login successful.");
        }

        return loginResponse;
    }
}