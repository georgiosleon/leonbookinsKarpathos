-- Normalize booking status values and enforce allowed status domain.

UPDATE Booking
SET status = lower(trim(status))
WHERE status IS NOT NULL;

UPDATE Booking
SET status = 'active'
WHERE status IS NULL OR trim(status) = '' OR lower(trim(status)) NOT IN ('active', 'paid', 'cancelled');

CREATE TABLE Booking_new (
    id varchar(255) NOT NULL,
    afm varchar(255),
    agency varchar(255),
    balance float,
    charge float,
    commission float,
    country varchar(255),
    email varchar(255),
    end_date varchar(255),
    extra_info varchar(255),
    identification varchar(255),
    invoice_number varchar(255),
    name varchar(255),
    num_of_guests integer,
    num_of_nights integer,
    received float,
    request_DateTime timestamp,
    room varchar(255),
    start_date varchar(255),
    status varchar(255) NOT NULL CHECK (status IN ('active', 'paid', 'cancelled')),
    tax_ref_number varchar(255),
    tel varchar(255),
    voucher varchar(255),
    user_id integer,
    PRIMARY KEY (id)
);

INSERT INTO Booking_new (
    id, afm, agency, balance, charge, commission, country, email, end_date, extra_info,
    identification, invoice_number, name, num_of_guests, num_of_nights, received,
    request_DateTime, room, start_date, status, tax_ref_number, tel, voucher, user_id
)
SELECT
    id, afm, agency, balance, charge, commission, country, email, end_date, extra_info,
    identification, invoice_number, name, num_of_guests, num_of_nights, received,
    request_DateTime, room, start_date, lower(trim(status)), tax_ref_number, tel, voucher, user_id
FROM Booking;

DROP TABLE Booking;
ALTER TABLE Booking_new RENAME TO Booking;

CREATE INDEX IF NOT EXISTS idx_booking_room_status ON Booking (room, status);
CREATE INDEX IF NOT EXISTS idx_booking_start_date ON Booking (start_date);
CREATE INDEX IF NOT EXISTS idx_booking_user_id ON Booking (user_id);
