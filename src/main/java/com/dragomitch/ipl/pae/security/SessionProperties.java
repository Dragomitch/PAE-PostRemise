package com.dragomitch.ipl.pae.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the session cookie ({@code app.session.*}).
 *
 * @param validity how long a session JWT (and its cookie) stays valid after sign-in
 *        ({@code app.session.validity}, default 12h)
 * @param cookieSecure whether the {@code session} and {@code XSRF-TOKEN} cookies carry the
 *        {@code Secure} attribute ({@code app.session.cookie-secure}, default false so that the
 *        application works over plain http locally; set it to true behind HTTPS)
 */
@ConfigurationProperties("app.session")
public record SessionProperties(
    @DefaultValue("12h") Duration validity,
    @DefaultValue("false") boolean cookieSecure) {
}
