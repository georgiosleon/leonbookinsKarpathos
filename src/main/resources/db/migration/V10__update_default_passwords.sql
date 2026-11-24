-- Update seeded default account passwords to the new default.
-- Only affects the standard default usernames; configured/custom usernames are not changed.
UPDATE users
SET password_hash = '$2a$10$gklJtQvI9IAtWj9Zdwn5beJfQYFa32lolSWrk1WMzwmrOdiJqQnwy'
WHERE username IN ('admin', 'user', 'guest');
