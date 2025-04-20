package com.batrawy.task.login.internal.resource.v1.handler;

import com.liferay.portal.kernel.model.User;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object to share data between handlers in the chain.
 */
public class LoginContext {
    private User user;
    private String email;
    private long companyId;
    private Map<String, Object> attributes = new HashMap<>();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
}