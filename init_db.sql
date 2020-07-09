CREATE SCHEMA IF NOT EXISTS notz_user;

CREATE TABLE IF NOT EXISTS notz_user.user(
    email VARCHAR PRIMARY KEY,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL
)
