package com.batrawy.task.login.internal.resource.v1.service;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;

import java.nio.charset.StandardCharsets;

/**
 * Service for CAPTCHA verification.
 */
@Component(service = CaptchaService.class)
public class CaptchaService {

    private static final Log _log = LogFactoryUtil.getLog(CaptchaService.class);

    // using recaptcha development phase secret key
    private static final String RECAPTCHA_SECRET = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyCaptcha(String captchaResponse) {
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
            return jsonResponse.getBoolean("success");
        }
        catch (Exception e) {
            _log.error("Error verifying CAPTCHA", e);
            return false;
        }
    }
}