-- ================================================================
--  RAILWAY MANAGEMENT SYSTEM — COMPLETE DATABASE SETUP
--  Run this file once in MySQL Workbench or MySQL CLI:
--    mysql -u root -p < railway_db.sql
-- ================================================================

-- Drop and recreate the database for a clean start
DROP DATABASE IF EXISTS railway_db;
CREATE DATABASE railway_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE railway_db;

-- ================================================================
-- TABLE: Train
-- Stores all trains in the railway network
-- ================================================================
CREATE TABLE Train (
    train_id     INT AUTO_INCREMENT PRIMARY KEY,
    train_name   VARCHAR(100) NOT NULL,
    train_type   ENUM('Express','Local','Freight') NOT NULL,
    total_seats  INT NOT NULL CHECK (total_seats >= 0),
    status       ENUM('Active','Inactive') DEFAULT 'Active'
);

-- ================================================================
-- TABLE: Station
-- Stores all railway stations with location info
-- ================================================================
CREATE TABLE Station (
    station_id      INT AUTO_INCREMENT PRIMARY KEY,
    station_name    VARCHAR(100) NOT NULL,
    city            VARCHAR(80)  NOT NULL,
    province        VARCHAR(80),
    platform_count  INT DEFAULT 1
);

-- ================================================================
-- TABLE: Route
-- Links two stations (source → destination) with distance & duration
-- Both FKs reference the same Station table
-- ================================================================
CREATE TABLE Route (
    route_id          INT AUTO_INCREMENT PRIMARY KEY,
    source_station_id INT NOT NULL,
    dest_station_id   INT NOT NULL,
    distance_km       DECIMAL(8,2),
    duration_mins     INT,
    FOREIGN KEY (source_station_id) REFERENCES Station(station_id) ON DELETE CASCADE,
    FOREIGN KEY (dest_station_id)   REFERENCES Station(station_id) ON DELETE CASCADE
);

-- ================================================================
-- TABLE: Schedule
-- Each row = one train running one route on a specific date/time
-- ================================================================
CREATE TABLE Schedule (
    schedule_id        INT AUTO_INCREMENT PRIMARY KEY,
    train_id           INT NOT NULL,
    route_id           INT NOT NULL,
    departure_datetime DATETIME NOT NULL,
    arrival_datetime   DATETIME NOT NULL,
    status             ENUM('OnTime','Delayed','Cancelled') DEFAULT 'OnTime',
    FOREIGN KEY (train_id)  REFERENCES Train(train_id)  ON DELETE CASCADE,
    FOREIGN KEY (route_id)  REFERENCES Route(route_id)  ON DELETE CASCADE
);

-- ================================================================
-- TABLE: Passenger
-- Stores registered passengers; email is used as login key
-- ================================================================
CREATE TABLE Passenger (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(120) NOT NULL,
    email        VARCHAR(150) UNIQUE NOT NULL,
    phone        VARCHAR(20),
    cnic         VARCHAR(20),
    age          INT CHECK (age BETWEEN 1 AND 120)
);

-- ================================================================
-- TABLE: Ticket
-- Junction table between Passenger and Schedule
-- Represents one seat booking
-- ================================================================
CREATE TABLE Ticket (
    ticket_id    INT AUTO_INCREMENT PRIMARY KEY,
    passenger_id INT NOT NULL,
    schedule_id  INT NOT NULL,
    seat_number  VARCHAR(10) NOT NULL,
    fare         DECIMAL(10,2) NOT NULL CHECK (fare > 0),
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status       ENUM('Confirmed','Cancelled','Pending') DEFAULT 'Confirmed',
    FOREIGN KEY (passenger_id) REFERENCES Passenger(passenger_id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id)  REFERENCES Schedule(schedule_id)   ON DELETE CASCADE
);

-- ================================================================
-- INDEXES — speed up the most common WHERE / JOIN lookups
-- ================================================================
CREATE INDEX idx_passenger_email    ON Passenger(email);
CREATE INDEX idx_schedule_departure ON Schedule(departure_datetime);
CREATE INDEX idx_ticket_status      ON Ticket(status);
CREATE INDEX idx_ticket_passenger   ON Ticket(passenger_id);
CREATE INDEX idx_ticket_schedule    ON Ticket(schedule_id);

-- ================================================================
-- VIEW: available_schedules
-- Pre-joins Schedule + Train + Route + Station and shows seats left
-- Java SearchService queries this view directly
-- ================================================================
CREATE VIEW available_schedules AS
SELECT
    s.schedule_id,
    t.train_name,
    t.train_type,
    src.station_name  AS from_station,
    dst.station_name  AS to_station,
    src.city          AS from_city,
    dst.city          AS to_city,
    s.departure_datetime,
    s.arrival_datetime,
    s.status          AS schedule_status,
    r.distance_km,
    r.duration_mins,
    (t.total_seats - COUNT(tk.ticket_id)) AS seats_left
FROM Schedule s
JOIN Train   t   ON s.train_id           = t.train_id
JOIN Route   r   ON s.route_id           = r.route_id
JOIN Station src ON r.source_station_id  = src.station_id
JOIN Station dst ON r.dest_station_id    = dst.station_id
LEFT JOIN Ticket tk ON tk.schedule_id    = s.schedule_id
                   AND tk.status         = 'Confirmed'
WHERE s.departure_datetime > NOW()
  AND s.status != 'Cancelled'
GROUP BY s.schedule_id, t.train_name, t.train_type,
         src.station_name, dst.station_name,
         src.city, dst.city,
         s.departure_datetime, s.arrival_datetime,
         s.status, r.distance_km, r.duration_mins, t.total_seats;

-- ================================================================
-- STORED PROCEDURE: BookTicket
-- Checks seat availability, then inserts ticket atomically
-- Called from Java using CallableStatement
-- ================================================================
DELIMITER $$ CREATE PROCEDURE BookTicket(
    IN  p_passenger_id INT,
    IN  p_schedule_id  INT,
    IN  p_seat_no      VARCHAR(10),
    IN  p_fare         DECIMAL(10,2),
    OUT p_result       VARCHAR(50)
)
BEGIN
    DECLARE seats_left INT DEFAULT 0;

    -- Start transaction to ensure atomic booking
    START TRANSACTION;

    -- Lock the schedule row so no one else can book the same seat simultaneously
    SELECT (t.total_seats - COUNT(tk.ticket_id)) INTO seats_left
    FROM   Schedule s
    JOIN   Train t    ON s.train_id    = t.train_id
    LEFT JOIN Ticket tk ON tk.schedule_id = s.schedule_id
                       AND tk.status = 'Confirmed'
    WHERE  s.schedule_id = p_schedule_id
    GROUP  BY t.total_seats
    FOR UPDATE; -- ADVANCED: Places an exclusive lock on these rows

    IF seats_left > 0 THEN
        INSERT INTO Ticket (passenger_id, schedule_id, seat_number, fare)
        VALUES (p_passenger_id, p_schedule_id, p_seat_no, p_fare);
        SET p_result = 'SUCCESS';
        COMMIT; -- Save changes and release lock
    ELSE
        SET p_result = 'NO_SEATS';
        ROLLBACK; -- Cancel operation and release lock
    END IF;
END$$ DELIMITER ;

-- ================================================================
-- SAMPLE DATA
-- ================================================================

-- 8 Trains
INSERT INTO Train (train_name, train_type, total_seats, status) VALUES
('Karachi Express',      'Express', 350, 'Active'),
('Lahore Mail',          'Express', 400, 'Active'),
('Quetta Jafar Express', 'Express', 300, 'Active'),
('Khyber Mail',          'Express', 380, 'Active'),
('Tezgam Express',       'Express', 320, 'Active'),
('Awam Express',         'Local',   250, 'Active'),
('Subak Raftar',         'Local',   200, 'Active'),
('Pakistan Freight',     'Freight',   0, 'Inactive');

-- 10 Stations
INSERT INTO Station (station_name, city, province, platform_count) VALUES
('Karachi City Station',  'Karachi',    'Sindh',               6),
('Lahore Junction',       'Lahore',     'Punjab',              8),
('Rawalpindi Station',    'Rawalpindi', 'Punjab',              5),
('Peshawar Cantonment',   'Peshawar',   'Khyber Pakhtunkhwa',  4),
('Quetta Station',        'Quetta',     'Balochistan',         3),
('Multan Cantonment',     'Multan',     'Punjab',              4),
('Faisalabad Station',    'Faisalabad', 'Punjab',              4),
('Hyderabad Junction',    'Hyderabad',  'Sindh',               3),
('Islamabad Station',     'Islamabad',  'Islamabad Capital',   4),
('Sukkur Station',        'Sukkur',     'Sindh',               3);

-- 10 Routes
INSERT INTO Route (source_station_id, dest_station_id, distance_km, duration_mins) VALUES
(1, 2, 1210, 750), -- Karachi   → Lahore
(2, 1, 1210, 750), -- Lahore    → Karachi
(2, 3,  360, 240), -- Lahore    → Rawalpindi
(3, 4,  170, 120), -- Rawalpindi→ Peshawar
(1, 5,  695, 480), -- Karachi   → Quetta
(2, 6,  330, 210), -- Lahore    → Multan
(6, 1,  870, 540), -- Multan    → Karachi
(1, 8,  160, 110), -- Karachi   → Hyderabad
(3, 9,   15,  25), -- Rawalpindi→ Islamabad
(1,10,  470, 310); -- Karachi   → Sukkur

-- 14 Schedules (future dates so view returns them)
INSERT INTO Schedule (train_id, route_id, departure_datetime, arrival_datetime, status) VALUES
(1, 1, '2026-07-10 08:00:00', '2026-07-10 20:30:00', 'OnTime'),
(1, 2, '2026-07-11 09:00:00', '2026-07-11 21:30:00', 'OnTime'),
(2, 3, '2026-07-12 07:00:00', '2026-07-12 11:00:00', 'OnTime'),
(2, 4, '2026-07-12 12:30:00', '2026-07-12 14:30:00', 'OnTime'),
(3, 5, '2026-07-13 18:00:00', '2026-07-14 02:00:00', 'OnTime'),
(4, 6, '2026-07-14 06:00:00', '2026-07-14 09:30:00', 'Delayed'),
(5, 7, '2026-07-15 10:00:00', '2026-07-15 19:00:00', 'OnTime'),
(6, 8, '2026-07-10 07:00:00', '2026-07-10 08:50:00', 'OnTime'),
(6, 8, '2026-07-11 07:00:00', '2026-07-11 08:50:00', 'OnTime'),
(7, 9, '2026-07-10 06:00:00', '2026-07-10 06:25:00', 'OnTime'),
(7, 9, '2026-07-10 14:00:00', '2026-07-10 14:25:00', 'OnTime'),
(1,10, '2026-07-16 09:00:00', '2026-07-16 14:10:00', 'OnTime'),
(2, 1, '2026-07-17 07:30:00', '2026-07-17 20:00:00', 'OnTime'),
(3, 5, '2026-07-20 18:00:00', '2026-07-21 02:00:00', 'Cancelled');

-- 15 Passengers
INSERT INTO Passenger (full_name, email, phone, cnic, age) VALUES
('Ahmed Khan',      'ahmed.khan@gmail.com',    '0301-1234567', '42201-1234567-1', 34),
('Fatima Zahra',    'fatima.zahra@yahoo.com',  '0312-2345678', '42101-2345678-2', 28),
('Muhammad Usman',  'm.usman@hotmail.com',     '0321-3456789', '35202-3456789-3', 45),
('Ayesha Siddiqui', 'ayesha.s@gmail.com',      '0333-4567890', '42301-4567890-4', 22),
('Bilal Ahmed',     'bilal.ahmed@gmail.com',   '0345-5678901', '35101-5678901-5', 30),
('Zainab Hussain',  'zainab.h@gmail.com',      '0311-6789012', '42501-6789012-6', 26),
('Tariq Mehmood',   'tariq.m@yahoo.com',       '0322-7890123', '35401-7890123-7', 52),
('Sana Malik',      'sana.malik@gmail.com',    '0344-8901234', '42201-8901234-8', 19),
('Umar Farooq',     'umar.farooq@gmail.com',   '0300-9012345', '35202-9012345-9', 38),
('Hina Nawaz',      'hina.nawaz@hotmail.com',  '0315-0123456', '54400-0123456-0', 41),
('Kamran Ali',      'kamran.ali@gmail.com',    '0333-1122334', '35101-1122334-1', 29),
('Rabia Noor',      'rabia.noor@yahoo.com',    '0321-2233445', '42101-2233445-2', 24),
('Imran Sheikh',    'imran.sheikh@gmail.com',  '0311-3344556', '35301-3344556-3', 47),
('Nadia Iqbal',     'nadia.iqbal@gmail.com',   '0302-4455667', '42501-4455667-4', 33),
('Saad Rehman',     'saad.rehman@gmail.com',   '0344-5566778', '35401-5566778-5', 21);

-- 20 Tickets
INSERT INTO Ticket (passenger_id, schedule_id, seat_number, fare, booking_date, status) VALUES
(1,  1, 'A-01', 2500.00, '2026-06-01 10:00:00', 'Confirmed'),
(2,  1, 'A-02', 2500.00, '2026-06-01 10:15:00', 'Confirmed'),
(3,  1, 'B-01', 1800.00, '2026-06-02 09:00:00', 'Confirmed'),
(4,  1, 'B-02', 1800.00, '2026-06-02 09:30:00', 'Cancelled'),
(5,  2, 'A-01', 2500.00, '2026-06-03 11:00:00', 'Confirmed'),
(6,  2, 'A-03', 2500.00, '2026-06-03 11:20:00', 'Confirmed'),
(7,  3, 'C-01',  900.00, '2026-06-04 08:00:00', 'Confirmed'),
(8,  3, 'C-02',  900.00, '2026-06-04 08:10:00', 'Confirmed'),
(9,  3, 'C-03',  900.00, '2026-06-04 08:20:00', 'Cancelled'),
(10, 4, 'D-01',  650.00, '2026-06-04 09:00:00', 'Confirmed'),
(11, 4, 'D-02',  650.00, '2026-06-04 09:05:00', 'Confirmed'),
(12, 5, 'E-01', 3200.00, '2026-06-05 14:00:00', 'Confirmed'),
(13, 5, 'E-02', 3200.00, '2026-06-05 14:30:00', 'Confirmed'),
(14, 6, 'F-01', 1100.00, '2026-06-06 07:00:00', 'Confirmed'),
(15, 6, 'F-02', 1100.00, '2026-06-06 07:15:00', 'Confirmed'),
(1,  8, 'G-01',  400.00, '2026-06-07 06:00:00', 'Confirmed'),
(3,  8, 'G-02',  400.00, '2026-06-07 06:10:00', 'Confirmed'),
(5, 10, 'H-01',  150.00, '2026-06-08 05:30:00', 'Confirmed'),
(7, 10, 'H-02',  150.00, '2026-06-08 05:45:00', 'Confirmed'),
(2, 13, 'A-05', 2500.00, '2026-06-09 08:00:00', 'Confirmed');