package com.leonsoft.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        String username = signedIn ? escapeAttribute(authentication.getName()) : "";
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
                      padding: 24px; background: linear-gradient(180deg, #eef4fb 0%%, #f7f9fc 100%%); color: #172033;
                      font: 16px/1.5 system-ui, sans-serif; }
                    main { width: min(100%%, 440px); padding: 32px; border: 1px solid #dbe4ef;
                      border-radius: 16px; background: #fff; box-shadow: 0 12px 40px rgba(23, 32, 51, 0.08); }
                    .welcome { margin: 0 0 10px; text-align: center; font-size: 1.8rem; font-weight: 800; letter-spacing: 0.04em; color: #1769aa; }
                    .login-image-rotator { position: relative; width: 100%%; max-width: 320px; aspect-ratio: 3 / 2; margin: 0 auto 14px; border-radius: 14px; overflow: hidden; background: #e9eef5; box-shadow: 0 4px 16px rgba(23,32,51,0.08); }
                    .login-image-rotator img { position: absolute; inset: 0; width: 100%%; height: 100%%; object-fit: cover; opacity: 0; transition: opacity 0.8s ease-in-out; }
                    .login-image-rotator img.active { opacity: 1; }
                    .brand { margin: 0 0 20px; text-align: center; font-size: 0.95rem; color: #5f6f85; }
                    h1 { margin: 0 0 4px; font-size: 1.5rem; }
                    p { color: #53627a; margin: 0 0 12px; }
                    .signed-in { color: #53627a; font-size: 0.95rem; }
                    label { display: block; margin-top: 14px; font-weight: 650; font-size: 0.95rem; }
                    input, select, button { width: 100%%; min-height: 44px; margin-top: 6px;
                      padding: 10px 12px; border: 1px solid #b7c4d6; border-radius: 10px;
                      font: inherit; background: #fff; }
                    input:focus, select:focus { outline: 3px solid rgba(23,105,170,0.18); border-color: #1769aa; }
                    button { margin-top: 22px; border: 0; background: #1769aa; color: white;
                      font-weight: 700; cursor: pointer; transition: background-color .15s ease, transform .02s ease; }
                    button:hover { background: #145c95; }
                    button:active { transform: translateY(1px); }
                    button:focus-visible { outline: 3px solid rgba(23,105,170,0.28); outline-offset: 2px; }
                    .error { color: #9e2424; background: #fff5f5; border: 1px solid #f0cccc; padding: 10px 12px; border-radius: 8px; }
                    .success { color: #1f6a3d; background: #f3fbf6; border: 1px solid #cfe8d8; padding: 10px 12px; border-radius: 8px; }
                    @media (max-width: 480px) { main { padding: 24px; } .welcome { font-size: 1.6rem; } }
                    @media (prefers-reduced-motion: reduce) { .login-image-rotator img, button { transition: none; } }
                  </style>
                </head>
                <body>
                  <main>
                    <p class="welcome" data-message="welcome">WELCOME</p>
                    <div class="login-image-rotator" aria-hidden="true">
                      <img src="/images/login-robot-greek-island.webp" alt="" class="active" loading="eager">
                      <img src="/images/login-anonymous-greek-island.webp" alt="" loading="lazy">
                      <img src="/images/Zoro’s Greek Island Postcard.webp" alt="" loading="lazy">
                      <img src="/images/Retro Greek Island Robot Postcard.webp" alt="" loading="lazy">
                      <img src="/images/Greek Island Pirate Adventure.webp" alt="" loading="lazy">
                      <img src="/images/Ninja at Santorini Sunset.webp" alt="" loading="lazy">
                      <img src="/images/Retro Action Hero Overlooking a Greek Island.webp" alt="" loading="lazy">
                      <img src="/images/Retro Commando Overlooking a Greek Island.webp" alt="" loading="lazy">
                      <img src="/images/Retro Greek Island Karate Poster.webp" alt="" loading="lazy">
                      <img src="/images/Ares_ Spartan Fury at Sunset.webp" alt="" loading="lazy">
                      <img src="/images/Red-Haired Warrior Over Greek Isles.webp" alt="" loading="lazy">
                      <img src="/images/Greek Island Arcane Mage.webp" alt="" loading="lazy">
                      <img src="/images/Zeus Above the Stormy Aegean.webp" alt="" loading="lazy">
                      <img src="/images/Aphrodite by the Golden Sea.webp" alt="" loading="lazy">
                      <img src="/images/Regal Hera with Peacock and Sunset.webp" alt="" loading="lazy">
                      <img src="/images/Dark Mage Beneath the Blood Moon.webp" alt="" loading="lazy">
                      <img src="/images/Dwarven Council in Karpathos.webp" alt="" loading="lazy">
                      <img src="/images/Halo Heroes Over Karpathos.webp" alt="" loading="lazy">
                      <img src="/images/StarCraft Battle Over Karpathos.webp" alt="" loading="lazy">
                      <img src="/images/Counter-Strike 2_ Karpathos Showdown.webp" alt="" loading="lazy">
                    </div>
                    <p class="brand" data-message="brand">Karpathos booking planner</p>
                    <p id="signed-in" class="signed-in" hidden data-user="%s">
                      <span data-message="signedIn">Signed in as</span> <span id="signed-in-name">%s</span>
                    </p>
                    <h1 data-message="title">Sign in</h1>
                    <p id="login-message" class="error" hidden data-message="invalid">Invalid username or password.</p>
                    <p id="logout-message" class="success" hidden data-message="signedOut">You have signed out.</p>
                    <form id="login-form" method="post" action="/login">
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
                      en: { welcome: 'WELCOME', brand: 'Karpathos booking planner', title: 'Sign in',
                        invalid: 'Invalid username or password.', signedOut: 'You have signed out.',
                        username: 'Username', password: 'Password', language: 'Language', submit: 'Sign in',
                        signedIn: 'Signed in as' },
                      el: { welcome: 'ΚΑΛΩΣ ΗΡΘΑΤΕ', brand: 'Σύστημα κρατήσεων Καρπάθου', title: 'Σύνδεση',
                        invalid: 'Λανθασμένο όνομα χρήστη ή κωδικός πρόσβασης.', signedOut: 'Έχετε αποσυνδεθεί.',
                        username: 'Όνομα χρήστη', password: 'Κωδικός πρόσβασης', language: 'Γλώσσα', submit: 'Σύνδεση',
                        signedIn: 'Συνδεδεμένος ως' },
                      de: { welcome: 'WILLKOMMEN', brand: 'Karpathos Buchungsplaner', title: 'Anmelden',
                        invalid: 'Ungültiger Benutzername oder ungültiges Passwort.', signedOut: 'Sie wurden abgemeldet.',
                        username: 'Benutzername', password: 'Passwort', language: 'Sprache', submit: 'Anmelden',
                        signedIn: 'Angemeldet als' },
                      nl: { welcome: 'WELKOM', brand: 'Boekingsplanner Karpathos', title: 'Aanmelden',
                        invalid: 'Ongeldige gebruikersnaam of wachtwoord.', signedOut: 'U bent afgemeld.',
                        username: 'Gebruikersnaam', password: 'Wachtwoord', language: 'Taal', submit: 'Aanmelden',
                        signedIn: 'Aangemeld als' },
                      da: { welcome: 'VELKOMMEN', brand: 'Bookingplanlægger for Karpathos', title: 'Log ind',
                        invalid: 'Ugyldigt brugernavn eller adgangskode.', signedOut: 'Du er logget ud.',
                        username: 'Brugernavn', password: 'Adgangskode', language: 'Sprog', submit: 'Log ind',
                        signedIn: 'Logget ind som' },
                      fr: { welcome: 'BIENVENUE', brand: 'Gestion des réservations de Karpathos', title: 'Connexion',
                        invalid: 'Nom d’utilisateur ou mot de passe invalide.', signedOut: 'Vous êtes déconnecté.',
                        username: 'Nom d’utilisateur', password: 'Mot de passe', language: 'Langue', submit: 'Se connecter',
                        signedIn: 'Connecté en tant que' }
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
                    const signedIn = document.getElementById('signed-in');
                    if (signedIn && signedIn.dataset.user) {
                      signedIn.hidden = false;
                    }
                    const rotatorImages = Array.from(document.querySelectorAll('.login-image-rotator img'));
                    if (rotatorImages.length > 1) {
                      let activeIndex = 0;
                      setInterval(() => {
                        rotatorImages[activeIndex].classList.remove('active');
                        activeIndex = (activeIndex + 1) %% rotatorImages.length;
                        rotatorImages[activeIndex].classList.add('active');
                      }, 3000);
                    }
                  </script>
                </body>
                </html>
                """.formatted(username, username,
                            escapeAttribute(csrfToken.getParameterName()), escapeAttribute(csrfToken.getToken()));
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
              .replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }
}
