package com.batrawy.task.login.internal.resource.v1;

import com.batrawy.task.login.dto.v1.LoginRequest;
import com.batrawy.task.login.dto.v1.LoginResponse;
import com.batrawy.task.login.resource.v1.LoginResource;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Validator;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Component(
        properties = "OSGI-INF/liferay/rest/v1/login.properties",
        scope = ServiceScope.PROTOTYPE, service = LoginResource.class
)
public class LoginResourceImpl extends BaseLoginResourceImpl {

    //---------------------------------------------------------
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int SUSPENSION_THRESHOLD = 5;
    // In seconds – these values should be coordinated with your cache config
    private static final int RATE_LIMIT_TTL = 60;
    private static final int FAILED_ATTEMPTS_TTL = 900;
    private static final int SUSPENSION_TTL = 60;
    //---------------------------------------------------------


    private PortalCache<String, Serializable> apiRateLimitCache;
    private PortalCache<String, Serializable> failedAttemptsCache;
    private PortalCache<String, Serializable> suspensionCache;

    private static final String API_RATE_LIMIT_CACHE_KEY = "login_api_rate_limit";
    private static final String FAILED_ATTEMPTS_CACHE_PREFIX = "failed_login_attempts_";
    private static final String SUSPENSION_CACHE_PREFIX = "user_suspension_";
    @Reference
    private UserLocalService userLocalService;

    @Reference
    private MultiVMPool multiVMPool;

    @Activate
    protected void activate() {
        apiRateLimitCache = (PortalCache<String, Serializable>)multiVMPool.getPortalCache("API_RATE_LIMIT_CACHE");
        failedAttemptsCache = (PortalCache<String, Serializable>)multiVMPool.getPortalCache("FAILED_ATTEMPTS_CACHE");
        suspensionCache = (PortalCache<String, Serializable>)multiVMPool.getPortalCache("SUSPENSION_CACHE");
    }


    private boolean isOverallRateLimitExceeded() {
        AtomicInteger requestCount = (AtomicInteger) apiRateLimitCache.get(API_RATE_LIMIT_CACHE_KEY);

        if (requestCount == null) {
            requestCount = new AtomicInteger(1);
            apiRateLimitCache.put(API_RATE_LIMIT_CACHE_KEY, requestCount, RATE_LIMIT_TTL);
            return false;
        }

        int currentCount = requestCount.incrementAndGet();
        apiRateLimitCache.put(API_RATE_LIMIT_CACHE_KEY, requestCount, RATE_LIMIT_TTL);

        return currentCount > MAX_REQUESTS_PER_MINUTE;
    }

    private boolean isUserSuspended(String email) {
        Boolean suspended = (Boolean) suspensionCache.get(SUSPENSION_CACHE_PREFIX + email);
        return suspended != null && suspended;
    }

    private int getFailedAttempts(String email) {
        Integer attempts = (Integer) failedAttemptsCache.get(FAILED_ATTEMPTS_CACHE_PREFIX + email);
        return attempts != null ? attempts : 0;
    }

    private void incrementFailedAttempts(String email) {
        int attempts = getFailedAttempts(email) + 1;
        failedAttemptsCache.put(FAILED_ATTEMPTS_CACHE_PREFIX + email, attempts, FAILED_ATTEMPTS_TTL);

        if (attempts >= SUSPENSION_THRESHOLD) {
            suspensionCache.put(SUSPENSION_CACHE_PREFIX + email, true, SUSPENSION_TTL);
        }
    }

    private void resetFailedAttempts(String email) {
        failedAttemptsCache.remove(FAILED_ATTEMPTS_CACHE_PREFIX + email);
    }

    private boolean requiresCaptcha(String email) {
        int attempts = getFailedAttempts(email);
        return attempts >= CAPTCHA_THRESHOLD;
    }
//----------------------------------------------------------------------------------------
    @Override
    public LoginResponse postLogin(LoginRequest loginRequest) throws Exception {
        LoginResponse loginResponse = new LoginResponse();

        // Validate the input
        if (Validator.isNull(loginRequest.getEmailAddress()) || Validator.isNull(loginRequest.getPassword())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Email and password are required.");
            return loginResponse;
        }

        // Check overall API rate limit
        if (isOverallRateLimitExceeded()) {
            loginResponse.setStatusCode(429);
            loginResponse.setStatusMessage("Too many requests. Please try again later.");
            return loginResponse;
        }

        String email = loginRequest.getEmailAddress();

        // Check if user is suspended
        if (isUserSuspended(email)) {
            loginResponse.setStatusCode(403);
            loginResponse.setStatusMessage("Account temporarily suspended due to too many failed login attempts. Please try again later.");
            return loginResponse;
        }

        // Check if CAPTCHA is required but not provided
        if (requiresCaptcha(email) && Validator.isNull(loginRequest.getCaptchaResponse())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("CAPTCHA verification required.");
            loginResponse.setRequireCaptcha(true);
            return loginResponse;
        }

        // If CAPTCHA is required, verify it
        if (requiresCaptcha(email) && !verifyCaptcha(loginRequest.getCaptchaResponse())) {
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("Invalid CAPTCHA response.");
            loginResponse.setRequireCaptcha(true);
            return loginResponse;
        }


        // Retrieve the user by email address
        User user;
        try {
            user = userLocalService.getUserByEmailAddress(contextCompany.getCompanyId(), loginRequest.getEmailAddress());
        } catch (Exception e) {
            // Increment failed attempts for non-existent users too (to prevent user enumeration)
            incrementFailedAttempts(email);
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
            incrementFailedAttempts(email);
            loginResponse.setStatusCode(400);
            loginResponse.setStatusMessage("TOTP code must be numeric.");
            return loginResponse;
        }

        // Verify the TOTP code
        boolean totpValid = gAuth.authorize(storedTotpSecret, totpCode);
        if (!totpValid) {
            incrementFailedAttempts(email);
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
                incrementFailedAttempts(email);
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

            // Reset failed attempts on successful login
            resetFailedAttempts(email);
        }

        return loginResponse;
    }

    // using recaptcha development phase secret key
    private static final String RECAPTCHA_SECRET = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private boolean verifyCaptcha(String captchaResponse) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Prepare the POST request to Google's reCAPTCHA verification endpoint
            HttpPost httpPost = new HttpPost(RECAPTCHA_VERIFY_URL);

            // Build the POST body in x-www-form-urlencoded format
            String requestBody = "secret=" + RECAPTCHA_SECRET + "&response=" + captchaResponse;
            StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
            entity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(entity);

            // Execute the request
            HttpResponse response = httpClient.execute(httpPost);
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            // Parse the JSON response
            JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(jsonString);
            boolean success = jsonResponse.getBoolean("success");

            return success;
        }
        catch (Exception e) {
            _log.error("Error verifying CAPTCHA", e);
            return false;
        }
    }
    private static final Log _log = LogFactoryUtil.getLog(LoginResourceImpl.class);
}