package com.bloodbridge;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.junit.jupiter.api.Assertions.*;

class AdminEnvironmentTest {
    private ApplicationContextRunner withEnvironment(String username, String hash) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("render-admin-test", Map.of(
                                "ADMIN_USERNAME", username,
                                "ADMIN_PASSWORD_HASH", hash))))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(AdminCredentials.class);
    }

    @Test
    void renderEnvironmentVariablesReachTheCredentialVerifier() throws Exception {
        byte[] salt = new byte[16];
        String hash = "pbkdf2:600000:" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(
                        AdminCredentials.derive("environment-test-password", salt, 600000));

        withEnvironment("environment-test-admin", hash).run(context -> {
            assertNull(context.getStartupFailure());
            AdminCredentials credentials = context.getBean(AdminCredentials.class);
            assertTrue(credentials.configured());
            assertEquals("environment-test-admin", credentials.username);
            assertTrue(credentials.accepts("environment-test-admin", "environment-test-password"));
            assertFalse(credentials.accepts("another-admin", "environment-test-password"));
            assertFalse(credentials.accepts("environment-test-admin", "incorrect-password"));
        });
    }

    @Test
    void missingConfigurationDoesNotEnableAnAdminAccount() {
        withEnvironment("", "").run(context -> {
            assertNull(context.getStartupFailure());
            AdminCredentials credentials = context.getBean(AdminCredentials.class);
            assertFalse(credentials.configured());
            assertFalse(credentials.accepts("environment-test-admin", "environment-test-password"));
        });
    }

    @Test
    void plaintextAndMalformedHashesCannotEnableAdminLogin() {
        for (String invalid : new String[]{"plaintext-is-not-a-hash", "pbkdf2:600000:AA==:AA=="}) {
            withEnvironment("environment-test-admin", invalid).run(context -> {
                assertNull(context.getStartupFailure());
                AdminCredentials credentials = context.getBean(AdminCredentials.class);
                assertFalse(credentials.configured());
                assertFalse(credentials.accepts("environment-test-admin", invalid));
            });
        }
    }
}
