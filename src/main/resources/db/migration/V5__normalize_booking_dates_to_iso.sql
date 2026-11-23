-- Normalize booking dates to ISO-8601 (YYYY-MM-DD).

UPDATE Booking
SET start_date = substr(start_date, 7, 4) || '-' || substr(start_date, 4, 2) || '-' || substr(start_date, 1, 2)
WHERE start_date LIKE '__/__/____';

UPDATE Booking
SET end_date = substr(end_date, 7, 4) || '-' || substr(end_date, 4, 2) || '-' || substr(end_date, 1, 2)
WHERE end_date LIKE '__/__/____';
