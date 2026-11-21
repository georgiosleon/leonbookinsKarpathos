package com.leonsoft.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginPageController {
    @GetMapping(value = "/login", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String loginPage(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title data-message="title">Sign in</title>
                  <style>
                    * { box-sizing: border-box; }
                    body { min-height: 100vh; margin: 0; display: grid; place-items: center;
                      padding: 20px; background: #f3f6fb; color: #172033;
                      font: 16px/1.5 system-ui, sans-serif; }
                    main { width: min(100%%, 420px); padding: 28px; border: 1px solid #d5deea;
                      border-radius: 14px; background: white; box-shadow: 0 8px 30px #17203318; }
                    h1 { margin: 0 0 8px; font-size: 1.5rem; }
                    p { color: #53627a; }
                    label { display: block; margin-top: 16px; font-weight: 650; }
                    input, select, button { width: 100%%; min-height: 44px; margin-top: 6px;
                      padding: 9px 11px; border: 1px solid #aeb9ca; border-radius: 8px;
                      font: inherit; }
                    button { margin-top: 22px; border: 0; background: #1769aa; color: white;
                      font-weight: 700; cursor: pointer; }
                    .error { color: #9e2424; }
                  </style>
                </head>
                <body>
                  <main>
                    <p data-message="brand">Karpathos booking planner</p>
                    <h1 data-message="title">Sign in</h1>
                    <p id="login-message" class="error" hidden data-message="invalid">Invalid username or password.</p>
                    <p id="logout-message" hidden data-message="signedOut">You have signed out.</p>
                    <form method="post" action="/login">
                      <input type="hidden" name="%s" value="%s">
                      <label><span data-message="username">Username</span>
                        <input name="username" autocomplete="username" required autofocus>
                      </label>
                      <label><span data-message="password">Password</span>
                        <input name="password" type="password" autocomplete="current-password" required>
                      </label>
                      <label><span data-message="language">Language</span>
                        <select id="language" aria-label="Language">
                          <option value="en">English</option>
                          <option value="el">Ελληνικά</option>
                          <option value="de">Deutsch</option>
                          <option value="nl">Nederlands</option>
                          <option value="da">Dansk</option>
                          <option value="fr">Français</option>
                        </select>
                      </label>
                      <button type="submit" data-message="submit">Sign in</button>
                    </form>
                  </main>
                  <script>
                    const copy = {
                      en: { brand: 'Karpathos booking planner', title: 'Sign in',
                        invalid: 'Invalid username or password.', signedOut: 'You have signed out.',
                        username: 'Username', password: 'Password', language: 'Language', submit: 'Sign in' },
                      el: { brand: 'Σύστημα κρατήσεων Καρπάθου', title: 'Σύνδεση',
                        invalid: 'Λανθασμένο όνομα χρήστη ή κωδικός πρόσβασης.', signedOut: 'Έχετε αποσυνδεθεί.',
                        username: 'Όνομα χρήστη', password: 'Κωδικός πρόσβασης', language: 'Γλώσσα', submit: 'Σύνδεση' },
                      de: { brand: 'Karpathos Buchungsplaner', title: 'Anmelden',
                        invalid: 'Ungültiger Benutzername oder ungültiges Passwort.', signedOut: 'Sie wurden abgemeldet.',
                        username: 'Benutzername', password: 'Passwort', language: 'Sprache', submit: 'Anmelden' },
                      nl: { brand: 'Boekingsplanner Karpathos', title: 'Aanmelden',
                        invalid: 'Ongeldige gebruikersnaam of wachtwoord.', signedOut: 'U bent afgemeld.',
                        username: 'Gebruikersnaam', password: 'Wachtwoord', language: 'Taal', submit: 'Aanmelden' },
                      da: { brand: 'Bookingplanlægger for Karpathos', title: 'Log ind',
                        invalid: 'Ugyldigt brugernavn eller adgangskode.', signedOut: 'Du er logget ud.',
                        username: 'Brugernavn', password: 'Adgangskode', language: 'Sprog', submit: 'Log ind' },
                      fr: { brand: 'Gestion des réservations de Karpathos', title: 'Connexion',
                        invalid: 'Nom d’utilisateur ou mot de passe invalide.', signedOut: 'Vous êtes déconnecté.',
                        username: 'Nom d’utilisateur', password: 'Mot de passe', language: 'Langue', submit: 'Se connecter' }
                    };
                    const selector = document.getElementById('language');
                    let language = 'en';
                    try {
                      const saved = localStorage.getItem('booking-language');
                      const detected = navigator.language.toLowerCase().split('-')[0];
                      language = Object.prototype.hasOwnProperty.call(copy, saved) ? saved
                        : (Object.prototype.hasOwnProperty.call(copy, detected) ? detected : 'en');
                    } catch {}
                    function applyLanguage(value) {
                      language = value;
                      document.documentElement.lang = value;
                      selector.value = value;
                      document.querySelectorAll('[data-message]').forEach((element) => {
                        element.textContent = copy[value][element.dataset.message];
                      });
                      const params = new URLSearchParams(location.search);
                      document.getElementById('login-message').hidden = !params.has('error');
                      document.getElementById('logout-message').hidden = !params.has('logout');
                      try { localStorage.setItem('booking-language', value); } catch {}
                    }
                    selector.addEventListener('change', () => applyLanguage(selector.value));
                    applyLanguage(language);
                  </script>
                </body>
                </html>
                """.formatted(escapeAttribute(csrfToken.getParameterName()), escapeAttribute(csrfToken.getToken()));
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
              .replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }
}
