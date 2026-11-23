ALTER TABLE booking_resources ADD COLUMN rental_tax_per_night integer NOT NULL DEFAULT 0;
ALTER TABLE Booking ADD COLUMN rental_tax integer NOT NULL DEFAULT 0;
