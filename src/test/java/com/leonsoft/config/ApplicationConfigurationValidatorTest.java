package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApplicationConfigurationValidatorTest {

    @Test
    void acceptsSupportedDeploymentTypesAndSqliteUrls() {
        assertDoesNotThrow(() ->
              ApplicationConfigurationValidator.validate("jdbc:sqlite:bookings.db", "standalone"));
        assertDoesNotThrow(() ->
              ApplicationConfigurationValidator.validate("jdbc:sqlite:/data/bookings.db", "docker"));
        assertDoesNotThrow(() ->
              ApplicationConfigurationValidator.validate("jdbc:sqlite:bookings.db", "desktop"));
    }

    @Test
    void rejectsUnsupportedOrIncompleteDatabaseConfiguration() {
        assertThrows(IllegalStateException.class, () ->
              ApplicationConfigurationValidator.validate(null, "standalone"));
        assertThrows(IllegalStateException.class, () ->
              ApplicationConfigurationValidator.validate("jdbc:postgresql://db/app", "standalone"));
        assertThrows(IllegalStateException.class, () ->
              ApplicationConfigurationValidator.validate("jdbc:sqlite:", "standalone"));
    }

    @Test
    void rejectsUnknownDeploymentType() {
        assertThrows(IllegalStateException.class, () ->
              ApplicationConfigurationValidator.validate("jdbc:sqlite:bookings.db", "portable"));
    }
}
