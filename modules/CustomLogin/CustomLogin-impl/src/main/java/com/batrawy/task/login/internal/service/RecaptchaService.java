package com.batrawy.task.login.internal.service;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;

import java.nio.charset.StandardCharsets;

/**
 * Service for reCAPTCHA verification
 */
@Component(
        immediate = true,
        service = RecaptchaService.class
)
public class RecaptchaService {

    private static final Log _log = LogFactoryUtil.getLog(RecaptchaService.class);

    // Using Google's test keys for development
    private static final String RECAPTCHA_SECRET = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyRecaptcha(String captchaResponse, String remoteIp) {
        if (Validator.isNull(captchaResponse)) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Prepare the POST request to Google's reCAPTCHA verification endpoint
            HttpPost httpPost = new HttpPost(RECAPTCHA_VERIFY_URL);

            // Build the POST body in x-www-form-urlencoded format
            StringBuilder requestBody = new StringBuilder();
            requestBody.append("secret=").append(RECAPTCHA_SECRET)
                    .append("&response=").append(captchaResponse);

            if (Validator.isNotNull(remoteIp)) {
                requestBody.append("&remoteip=").append(remoteIp);
            }

            StringEntity entity = new StringEntity(requestBody.toString(), StandardCharsets.UTF_8);
            entity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(entity);

            // Execute the request
            HttpResponse response = httpClient.execute(httpPost);
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            // Parse the JSON response
            JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(jsonString);
            return jsonResponse.getBoolean("success");
        } catch (Exception e) {
            _log.error("Error verifying CAPTCHA", e);
            return false;
        }
    }
}