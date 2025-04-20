package com.batrawy.task.login.internal.resource.v1.auth;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.internal.resource.v1.handler.LoginContext;
import com.batrawy.task.login.internal.resource.v1.service.FailedAttemptsService;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Authentication using Liferay's built-in headless API.
 */
public class LiferayApiAuthenticationStrategy implements AuthenticationStrategy {

    private static final Log _log = LogFactoryUtil.getLog(LiferayApiAuthenticationStrategy.class);
    private final FailedAttemptsService failedAttemptsService;
    private final String liferayApiUrl;

    public LiferayApiAuthenticationStrategy(FailedAttemptsService failedAttemptsService, String liferayApiUrl) {
        this.failedAttemptsService = failedAttemptsService;
        this.liferayApiUrl = liferayApiUrl;
    }

    @Override
    public boolean authenticate(LoginRequest request, LoginResponse response, LoginContext context) {
        String email = context.getEmail();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create the GET request
            HttpGet httpGet = new HttpGet(liferayApiUrl);

            // Add Basic Authentication header
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(email, request.getPassword());
            httpGet.addHeader(new BasicScheme().authenticate(creds, httpGet, null));

            // Execute the request
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(httpResponse.getEntity());

            if (statusCode != 200) {
                failedAttemptsService.incrementFailedAttempts(email);
                response.setStatusCode(statusCode);
                response.setStatusMessage("Email or Password is Wrong, please try again.");
                return false;
            }

            // Parse the responseBody
            JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(responseBody);
            response.setUserId(jsonResponse.getLong("id"));
            response.setScreenName(jsonResponse.getString("name"));

            // Reset failed attempts on successful login
            failedAttemptsService.resetFailedAttempts(email);

            return true;
        } catch (Exception e) {
            _log.error("Error during Liferay API authentication", e);
            response.setStatusCode(500);
            response.setStatusMessage("Internal server error during authentication.");
            return false;
        }
    }
}