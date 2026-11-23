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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
        RequestMatcher bookingEndpoints = request -> {
            String path = request.getServletPath();
            return path.equals("/booking") || path.startsWith("/booking/")
                  || path.equals("/api/v1/booking") || path.startsWith("/api/v1/booking/")
                  || path.equals("/api/v1/resources") || path.startsWith("/api/v1/resources/");
        };

        http
              .authorizeHttpRequests(authorize -> authorize
                    // Public SPA shell, static assets, and login/error pages.
                    .requestMatchers("/", "/index.html", "/favicon.ico", "/error",
                                     "/rooms.html", "/cars.html",
                                     "/css/**", "/js/**", "/react-assets/**").permitAll()
                    .requestMatchers("/login", "/logout").permitAll()
                    .requestMatchers("/api/v1/csrf", "/csrf").authenticated()
                    // The current user is available to every authenticated user.
                    .requestMatchers("/api/v1/users/me").authenticated()
                    // User administration is restricted to admins.
                    .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
                    // Read-only booking/resource/settings data for all authenticated roles.
                    .requestMatchers(HttpMethod.GET, "/api/v1/booking/getAll", "/booking/getAll")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    .requestMatchers(HttpMethod.GET, "/api/v1/resources")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    .requestMatchers(HttpMethod.GET, "/api/v1/settings/availability")
                        .hasAnyRole("ADMIN", "OPERATOR", "GUEST")
                    // Reports/export are available to admins and operators; guests are read-only.
                    .requestMatchers(HttpMethod.GET,
                                     "/api/v1/booking/report", "/booking/report",
                                     "/api/v1/booking/export/pdf", "/booking/export/pdf")
                        .hasAnyRole("ADMIN", "OPERATOR")
                    // Booking mutations are operator-only. Admins are system-only.
                    .requestMatchers(HttpMethod.POST,
                                     "/api/v1/booking/save", "/booking/save",
                                     "/api/v1/booking/delete", "/booking/delete",
                                     "/api/v1/booking/del", "/booking/del")
                        .hasRole("OPERATOR")
                    .requestMatchers(HttpMethod.PUT,
                                     "/api/v1/booking/update", "/booking/update")
                        .hasRole("OPERATOR")
                    // Resource management is admin-only.
                    .requestMatchers(HttpMethod.POST, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/resources", "/api/v1/resources/**")
                        .hasRole("ADMIN")
                    // Operational endpoints: version visible to authenticated users, diagnostics admin-only.
                    .requestMatchers("/api/v1/app-info").authenticated()
                    .requestMatchers("/api/v1/diagnostics").hasRole("ADMIN")
                    // Deny-by-default for any unmapped endpoint.
                    .anyRequest().denyAll())
              .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/")
                    .permitAll())
              .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((request, response, authentication) ->
                          response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .permitAll())
              .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(csrfTokenRequestHandler))
              .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                          new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                          bookingEndpoints));

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
}
