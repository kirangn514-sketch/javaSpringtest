package com.example.studentapp.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings from application.properties under the "app.jwt" prefix.
 *
 * @ConfigurationProperties binds the values to this record's fields by name:
 *   app.jwt.expiry-minutes  →  expiryMinutes
 *   app.jwt.cookie-secure   →  cookieSecure
 *
 * @Validated makes Spring check the limits at startup. A bad value stops the app with a clear message
 * instead of creating tokens with a wrong lifetime.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @Min(value = 15, message = "app.jwt.expiry-minutes must be at least 15")
        @Max(value = 60, message = "app.jwt.expiry-minutes must be at most 60")
        int expiryMinutes,

        boolean cookieSecure
) {
}
