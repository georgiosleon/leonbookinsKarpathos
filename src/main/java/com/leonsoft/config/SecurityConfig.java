package com.leonsoft.config;

import com.leonsoft.repositories.UserRepository;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final LicenseEnforcementFilter licenseEnforcementFilter;

    public SecurityConfig(UserRepository userRepository, LicenseEnforcementFilter licenseEnforcementFilter) {
        this.userRepository = userRepository;
        this.licenseEnforcementFilter = licenseEnforcementFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
        RequestMatcher apiEndpoints = request -> {
            String path = request.getServletPath();
            return path != null && path.startsWith("/api/v1/");
        };

        http
              .authorizeHttpRequests(authorize -> authorize
                    // Public static assets and login/error pages.
                    .requestMatchers("/favicon.ico", "/error",
                                     "/css/**", "/js/**", "/react-assets/**", "/images/**").permitAll()
                    .requestMatchers("/login", "/logout").permitAll()
                    // SPA shells require authentication; form login redirects unauthenticated users.
                    .requestMatchers("/", "/index.html", "/rooms.html", "/cars.html").authenticated()
                    .requestMatchers("/api/v1/csrf").authenticated()
                    // The current user is available to every authenticated user.
                    .requestMatchers("/api/v1/users/me").authenticated()
                    // User administration is restricted to admins.
                    .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                    // Licensing: status visible to authenticated users, activation/admin ops restricted.
                    .requestMatchers(HttpMethod.GET, "/api/v1/license/status").authenticated()
                    .requestMatchers("/api/v1/license/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/customers", "/api/v1/customers/**").hasRole("ADMIN")
                    // Read-only booking/resource/settings data for all authenticated roles.
                    .requestMatchers(HttpMethod.GET, "/api/v1/booking/getAll")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    .requestMatchers(HttpMethod.GET, "/api/v1/resources")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    .requestMatchers(HttpMethod.GET, "/api/v1/settings/availability")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    // Reports/export are available to admins and operators; guests are read-only.
                    .requestMatchers(HttpMethod.GET,
                                     "/api/v1/booking/report",
                                     "/api/v1/booking/export/pdf")
                        .hasAnyRole("ADMIN", "OPERATOR")
                    // Booking mutations are available to admins and operators.
                    .requestMatchers(HttpMethod.POST,
                                     "/api/v1/booking/save",
                                     "/api/v1/booking/delete",
                                     "/api/v1/booking/del")
                        .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers(HttpMethod.PUT,
                                     "/api/v1/booking/update")
                        .hasAnyRole("ADMIN", "OPERATOR")
                    // Resource management is admin-only.
                    .requestMatchers(HttpMethod.POST, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    // Operational endpoints: visible to authenticated users.
                    .requestMatchers("/api/v1/app-info", "/api/v1/diagnostics").authenticated()
                    // Deny-by-default for any unmapped endpoint.
                    .anyRequest().denyAll())
              .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .permitAll())
              .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((request, response, authentication) ->
                          response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .permitAll())
              .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(csrfTokenRequestHandler))
              .addFilterAfter(licenseEnforcementFilter, AnonymousAuthenticationFilter.class)
              .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                          (request, response, authException) -> writeSecurityError(response, request, HttpStatus.UNAUTHORIZED, "Authentication required."),
                          apiEndpoints)
                    .defaultAccessDeniedHandlerFor(
                          (request, response, accessDeniedException) -> writeSecurityError(response, request, HttpStatus.FORBIDDEN, "Access denied."),
                          apiEndpoints));

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository
                .findByUsername(username)
                .map(user -> User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .authorities(user.getAuthorities())
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeSecurityError(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        String path = request.getRequestURI() == null ? "" : request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = "{\"timestamp\":\""
              + Instant.now()
              + "\",\"status\":" + status.value()
              + ",\"error\":\"" + status.getReasonPhrase()
              + "\",\"message\":\"" + safeMessage
              + "\",\"path\":\"" + path + "\"}";
        response.getWriter().write(body);
    }
}
