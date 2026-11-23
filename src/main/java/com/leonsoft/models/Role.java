package com.leonsoft.models;

import java.util.Arrays;
import java.util.Locale;

/**
 * Canonical application roles. Values are stored in the database as the
 * uppercase names so that Spring Security expressions such as
 * {@code hasRole('ADMIN')} match the authorities produced by
 * {@link User#getAuthorities()}.
 */
public enum Role {
    ADMIN, OPERATOR, GUEST;

    /**
     * Returns the canonical role for a stored value. Unknown or blank values
     * default to {@link #OPERATOR}, so every user has a meaningful role.
     */
    public static Role from(String value) {
        if (value != null) {
            String normalized = value.strip().toUpperCase(Locale.ROOT);
            for (Role role : values()) {
                if (role.name().equals(normalized)) {
                    return role;
                }
            }
        }
        return OPERATOR;
    }

    /** True when the stored value names a known role (case-insensitive). */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).anyMatch(role -> role.name().equals(normalized));
    }
}
