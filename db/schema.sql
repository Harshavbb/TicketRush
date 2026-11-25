-- TicketRush schema (MySQL 8+, InnoDB)
-- Run order matters: tables are created in foreign-key dependency order.

CREATE DATABASE IF NOT EXISTS ticketrush
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ticketrush;

-- ---------------------------------------------------------------------------
-- Static catalogue tables
-- ---------------------------------------------------------------------------

CREATE TABLE theatre (
    theatre_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    theatre_name VARCHAR(100) NOT NULL,
    location     VARCHAR(200) NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE screen (
    screen_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    theatre_id  BIGINT NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    capacity    INT NOT NULL,
    CONSTRAINT chk_capacity CHECK (capacity > 0),
    CONSTRAINT uq_screen_theatre_name UNIQUE (theatre_id, screen_name),
    CONSTRAINT fk_screen_theatre FOREIGN KEY (theatre_id) REFERENCES theatre(theatre_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE seat (
    seat_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id   BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    seat_type   VARCHAR(50) NOT NULL,
    CONSTRAINT uq_seat_screen_number UNIQUE (screen_id, seat_number),
    CONSTRAINT fk_seat_screen FOREIGN KEY (screen_id) REFERENCES screen(screen_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE movie (
    movie_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    language         VARCHAR(50) NOT NULL,
    CONSTRAINT chk_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB;

CREATE TABLE movie_show (
    show_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    screen_id  BIGINT NOT NULL,
    movie_id   BIGINT NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time   DATETIME(6) NOT NULL,
    CONSTRAINT chk_show_time CHECK (end_time > start_time),
    CONSTRAINT fk_show_screen FOREIGN KEY (screen_id) REFERENCES screen(screen_id),
    CONSTRAINT fk_show_movie FOREIGN KEY (movie_id) REFERENCES movie(movie_id),
    INDEX idx_show_screen_time (screen_id, start_time, end_time)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Concurrency and idempotency tables
-- ---------------------------------------------------------------------------

CREATE TABLE booking (
    booking_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(32) NOT NULL,
    user_id           BIGINT NOT NULL,
    show_id           BIGINT NOT NULL,
    idempotency_key   VARCHAR(64) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount      DECIMAL(10, 2) NOT NULL,
    expires_at        DATETIME(6) NOT NULL,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_booking_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_booking_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUND_REQUIRED')
    ),
    CONSTRAINT uq_booking_reference UNIQUE (booking_reference),
    CONSTRAINT uq_booking_user_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_booking_show FOREIGN KEY (show_id) REFERENCES movie_show(show_id),
    INDEX idx_booking_status_expires (status, expires_at)
) ENGINE=InnoDB;

CREATE TABLE show_seat (
    show_seat_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id              BIGINT NOT NULL,
    seat_id              BIGINT NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    price                DECIMAL(10, 2) NOT NULL,
    locked_by_booking_id BIGINT NULL,
    locked_until         DATETIME(6) NULL,
    version              INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_show_seat_price CHECK (price >= 0),
    CONSTRAINT chk_show_seat_status CHECK (
        status IN ('AVAILABLE', 'LOCKED', 'BOOKED')
    ),
    CONSTRAINT uq_show_seat_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT fk_show_seat_show FOREIGN KEY (show_id) REFERENCES movie_show(show_id) ON DELETE CASCADE,
    CONSTRAINT fk_show_seat_seat FOREIGN KEY (seat_id) REFERENCES seat(seat_id),
    CONSTRAINT fk_show_seat_booking FOREIGN KEY (locked_by_booking_id) REFERENCES booking(booking_id),
    INDEX idx_show_seat_lookup (show_id, status, seat_id, price, locked_until)
) ENGINE=InnoDB;

CREATE TABLE booking_seat (
    booking_seat_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id       BIGINT NOT NULL,
    show_seat_id     BIGINT NOT NULL,
    price_at_booking DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_booking_seat_price CHECK (price_at_booking >= 0),
    CONSTRAINT uq_booking_seat_pair UNIQUE (booking_id, show_seat_id),
    CONSTRAINT fk_booking_seat_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seat_show_seat FOREIGN KEY (show_seat_id) REFERENCES show_seat(show_seat_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Payment tables (schema ready; payment API not implemented yet)
-- ---------------------------------------------------------------------------

CREATE TABLE payment (
    payment_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id         BIGINT NOT NULL,
    idempotency_key    VARCHAR(64) NOT NULL,
    gateway_order_id   VARCHAR(100) NULL,
    gateway_payment_id VARCHAR(100) NULL,
    amount             DECIMAL(10, 2) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED')
    ),
    CONSTRAINT uq_payment_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_payment_gateway_order UNIQUE (gateway_order_id),
    CONSTRAINT uq_payment_gateway_payment UNIQUE (gateway_payment_id),
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
) ENGINE=InnoDB;

CREATE TABLE processed_webhook (
    event_id     VARCHAR(128) PRIMARY KEY,
    booking_id   BIGINT NOT NULL,
    event_type   VARCHAR(64) NOT NULL,
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_webhook_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
) ENGINE=InnoDB;