package com.batrawy.task.login.internal.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true,
        service = AuthenticationStrategy.class
)
public class LiferayBasicAuthStrategy implements AuthenticationStrategy {

    private static final Log _log = LogFactoryUtil.getLog(LiferayBasicAuthStrategy.class);
    private static final String BUILT_IN_LOGIN_URL = "http://localhost:8080/o/headless-admin-user/v1.0/my-user-account";

    @Override
    public boolean authenticate(LoginRequest loginRequest, User user, LoginResponse loginResponse) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create the GET request
            HttpGet httpGet = new HttpGet(BUILT_IN_LOGIN_URL);

            // Add Basic Authentication header
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
                    loginRequest.getEmailAddress(), loginRequest.getPassword());
            httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet, null));

            // Execute the request
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(httpResponse.getEntity());

            if (statusCode != 200) {
                loginResponse.setStatusCode(statusCode);
                loginResponse.setStatusMessage("Authentication failed via built-in API.");
                return false;
            }

            // Parse the responseBody
            JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(responseBody);
            loginResponse.setUserId(jsonResponse.getLong("id"));
            loginResponse.setScreenName(jsonResponse.getString("name"));
            loginResponse.setStatusCode(200);
            loginResponse.setStatusMessage("Login successful.");

            return true;
        } catch (Exception e) {
            _log.error("Error during Liferay basic authentication", e);
            loginResponse.setStatusCode(500);
            loginResponse.setStatusMessage("Internal server error during authentication.");
            return false;
        }
    }
}