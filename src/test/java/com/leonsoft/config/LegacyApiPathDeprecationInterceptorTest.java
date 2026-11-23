package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LegacyApiPathDeprecationInterceptorTest {

    private final LegacyApiPathDeprecationInterceptor interceptor = new LegacyApiPathDeprecationInterceptor();

    @Test
    void marksLegacyBookingPathAsDeprecated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/booking/getAll");
        request.setRequestURI("/booking/getAll");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertTrue(proceed);
        assertEquals("true", response.getHeader("Deprecation"));
        assertTrue(response.getHeader("Warning").contains("/api/v1/"));
    }

    @Test
    void doesNotMarkVersionedApiPathAsDeprecated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/booking/getAll");
        request.setRequestURI("/api/v1/booking/getAll");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertFalse(response.containsHeader("Deprecation"));
    }

    @Test
    void marksLegacyCsrfPathAsDeprecated() {
        assertTrue(LegacyApiPathDeprecationInterceptor.isLegacyPath("/csrf"));
        assertFalse(LegacyApiPathDeprecationInterceptor.isLegacyPath("/api/v1/csrf"));
    }
}
