CREATE TABLE customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    telephone VARCHAR(100),
    email VARCHAR(255),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE TABLE app_installation (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    install_id VARCHAR(64) NOT NULL UNIQUE,
    created_at timestamp NOT NULL
);

CREATE TABLE app_license (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    install_id VARCHAR(64) NOT NULL,
    customer_id INTEGER,
    signed_license TEXT NOT NULL,
    activated_at timestamp NOT NULL,
    activated_by VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);
