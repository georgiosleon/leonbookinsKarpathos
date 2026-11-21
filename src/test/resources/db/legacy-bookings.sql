CREATE TABLE Booking (
    id varchar(255) not null primary key,
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
    room varchar(255),
    start_date varchar(255),
    status varchar(255),
    tax_ref_number varchar(255),
    tel varchar(255),
    voucher varchar(255),
    request_DateTime timestamp
);

INSERT INTO Booking (id, name, room, start_date, end_date, status)
VALUES ('legacy-booking-1', 'Existing Customer', 'APT1', '2025-07-10', '2025-07-14', 'active');
