package com.leonsoft.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.leonsoft.services.LicenseService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

class LicenseEnforcementFilterTest {

    private LicenseService licenseService;
    private LicenseEnforcementFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        licenseService = mock(LicenseService.class);
        filter = new LicenseEnforcementFilter(licenseService);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsApiWhenActivated() throws Exception {
        when(licenseService.isActivated()).thenReturn(true);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/booking/getAll");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    void blocksNonWhitelistedApiWhenUnlicensed() throws Exception {
        when(licenseService.isActivated()).thenReturn(false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/booking/getAll");
        req.setServletPath("/api/v1/booking/getAll");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("License activation required"));
        assertTrue(res.getContentAsString().contains("/api/v1/booking/getAll"));
    }

    @Test
    void allowsWhitelistedPathsWhenUnlicensed() throws Exception {
        when(licenseService.isActivated()).thenReturn(false);
        for (String path : List.of("/api/v1/csrf", "/api/v1/users/me", "/api/v1/license/status", "/login", "/css/app.css", "/images/login-image-1.jpg", "/")) {
            FilterChain c = mock(FilterChain.class);
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            req.setServletPath(path);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, c);
            verify(c, description(path)).doFilter(req, res);
        }
    }

    @Test
    void allowsCustomerApiOnlyForAdminWhenUnlicensed() throws Exception {
        when(licenseService.isActivated()).thenReturn(false);

        MockHttpServletRequest adminReq = new MockHttpServletRequest("GET", "/api/v1/customers");
        adminReq.setServletPath("/api/v1/customers");
        MockHttpServletResponse adminRes = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        FilterChain adminChain = mock(FilterChain.class);
        filter.doFilter(adminReq, adminRes, adminChain);
        verify(adminChain).doFilter(adminReq, adminRes);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("op", "x", List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))));
        MockHttpServletRequest opReq = new MockHttpServletRequest("GET", "/api/v1/customers");
        opReq.setServletPath("/api/v1/customers");
        MockHttpServletResponse opRes = new MockHttpServletResponse();
        FilterChain opChain = mock(FilterChain.class);
        filter.doFilter(opReq, opRes, opChain);
        verify(opChain, never()).doFilter(any(), any());
        assertEquals(403, opRes.getStatus());
    }
}
