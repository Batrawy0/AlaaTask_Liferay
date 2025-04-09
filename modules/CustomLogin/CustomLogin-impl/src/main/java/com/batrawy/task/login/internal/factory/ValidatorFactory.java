package com.batrawy.task.login.internal.factory;

import com.batrawy.task.login.internal.validator.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Factory for creating validator chains
 */
@Component(
        immediate = true,
        service = ValidatorFactory.class
)
public class ValidatorFactory {

    @Reference
    private InputValidator inputValidator;

    @Reference
    private RateLimitValidator rateLimitValidator;

    @Reference
    private AccountStatusValidator accountStatusValidator;

    @Reference
    private CaptchaValidator captchaValidator;

    /**
     * Creates a chain of validators for login validation
     *
     * @return The first validator in the chain
     */
    public LoginValidator createLoginValidatorChain() {
        inputValidator.setNext(rateLimitValidator)
                .setNext(accountStatusValidator)
                .setNext(captchaValidator);

        return inputValidator;
    }
}