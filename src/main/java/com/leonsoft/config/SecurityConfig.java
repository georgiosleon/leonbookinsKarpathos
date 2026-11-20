package com.leonsoft.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

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
                    .requestMatchers(
                          "/booking/**", "/api/v1/booking/**",
                          "/api/v1/resources", "/api/v1/resources/**")
                    .hasRole("ADMIN")
                    .anyRequest().authenticated())
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
              .exceptionHandling(exceptions -> exceptions
                    .defaultAuthenticationEntryPointFor(
                          new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                          bookingEndpoints));

        return http.build();
    }

    @Bean
    UserDetailsService adminUserDetailsService(
          @Value("${app.admin.username}") String username,
          @Value("${app.admin.password}") String password,
          PasswordEncoder passwordEncoder) {



        if (username.isBlank()) {
            throw new IllegalStateException("APP_ADMIN_USERNAME must not be blank.");
        }
        int passwordLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordLength < 12 || passwordLength > 72) {
            throw new IllegalStateException("APP_ADMIN_PASSWORD must be between 12 and 72 UTF-8 bytes.");
        }

        return new InMemoryUserDetailsManager(User.withUsername(username)
              .password(passwordEncoder.encode(password))
              .roles("ADMIN")
              .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
