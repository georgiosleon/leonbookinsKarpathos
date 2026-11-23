CREATE TABLE IF NOT EXISTS Booking (
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
    status varchar(255),
    tax_ref_number varchar(255),
    tel varchar(255),
    voucher varchar(255),
    user_id integer,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS booking_resources (
    id varchar(255) NOT NULL,
    active boolean NOT NULL,
    name varchar(255) NOT NULL,
    resource_type varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
    id integer,
    is_active boolean NOT NULL,
    created_at timestamp,
    email varchar(255),
    password_hash varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    username varchar(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);
