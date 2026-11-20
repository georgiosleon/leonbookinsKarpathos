package com.leonsoft.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

class LoginPageControllerTest {
    @Test
    void providesCsrfProtectedLoginFormAndEscapesTokenValues() {
        CsrfToken csrfToken = org.mockito.Mockito.mock(CsrfToken.class);
        when(csrfToken.getParameterName()).thenReturn("_csrf");
        when(csrfToken.getToken()).thenReturn("token\"&value");

        String page = new LoginPageController().loginPage(csrfToken);

        assertTrue(page.contains("name=\"_csrf\" value=\"token&quot;&amp;value\""));
        assertTrue(page.contains("Invalid username or password."));
        assertTrue(page.contains("Λανθασμένο όνομα χρήστη ή κωδικός πρόσβασης."));
        assertTrue(page.contains("localStorage.setItem('booking-language', value)"));
    }
}
