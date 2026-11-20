CREATE TABLE IF NOT EXISTS booking_resources (
    id TEXT PRIMARY KEY,
    resource_type TEXT NOT NULL CHECK (resource_type IN ('rooms', 'cars')),
    name TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_resources_type_name
    ON booking_resources(resource_type, name COLLATE NOCASE);

INSERT OR IGNORE INTO booking_resources (id, resource_type, name) VALUES
    ('APT2', 'rooms', 'APT 2'),
    ('APT5', 'rooms', 'APT 5'),
    ('APT6', 'rooms', 'APT 6'),
    ('STU7', 'rooms', 'STU 7'),
    ('STU8', 'rooms', 'STU 8'),
    ('STU9', 'rooms', 'STU 9'),
    ('STU10', 'rooms', 'STU 10'),
    ('STU11', 'rooms', 'STU 11'),
    ('STU12', 'rooms', 'STU 12'),
    ('POT', 'rooms', 'POTIDEION'),
    ('MER', 'rooms', 'MERTONAS'),
    ('CAR1', 'cars', 'CAR 1 🚗'),
    ('CAR2', 'cars', 'CAR 2 🚗'),
    ('CAR3', 'cars', 'CAR 3 🚗'),
    ('CAR4', 'cars', 'CAR 4 🚗'),
    ('CAR5', 'cars', 'CAR 5 🚗'),
    ('CAR6', 'cars', 'CAR 6 🚗'),
    ('CAR7', 'cars', 'CAR 7 🚗'),
    ('CAR8', 'cars', 'CAR 8 🚗'),
    ('BIKE1', 'cars', 'BIKE 1 🛵'),
    ('BIKE2', 'cars', 'BIKE 2 🛵'),
    ('BIKE3', 'cars', 'BIKE 3 🛵'),
    ('BIKE4', 'cars', 'BIKE 4 🛵'),
    ('BIKE5', 'cars', 'BIKE 5 🛵'),
    ('BIKE6', 'cars', 'BIKE 6 🛵');
