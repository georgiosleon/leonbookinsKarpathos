package com.leonsoft.config;

import java.util.Locale;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApplicationConfigurationValidator implements ApplicationRunner {
    private static final Set<String> DEPLOYMENT_TYPES = Set.of("desktop", "docker", "standalone");

    private final Environment environment;

    public ApplicationConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        validate(environment.getProperty("url"), environment.getProperty("app.deployment-type"));
    }

    static void validate(String databaseUrl, String deploymentType) {
        if (databaseUrl == null || !databaseUrl.startsWith("jdbc:sqlite:")
              || databaseUrl.substring("jdbc:sqlite:".length()).isBlank()) {
            throw new IllegalStateException(
                  "Database configuration must use a non-empty SQLite JDBC URL (jdbc:sqlite:<path>).");
        }
        String normalizedType = deploymentType == null
              ? ""
              : deploymentType.trim().toLowerCase(Locale.ROOT);
        if (!DEPLOYMENT_TYPES.contains(normalizedType)) {
            throw new IllegalStateException(
                  "APP_DEPLOYMENT_TYPE must be one of: desktop, docker, standalone.");
        }
    }
}
