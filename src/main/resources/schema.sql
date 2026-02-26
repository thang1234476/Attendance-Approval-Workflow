-- Tạo bảng User
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telegram_id VARCHAR(50) UNIQUE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tạo bảng SystemConfig
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL
);

-- Tạo bảng Attendance
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    check_in_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tạo bảng LeaveRequest
CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL,
    approved_by BIGINT,
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Insert default System Configs
INSERT IGNORE INTO system_config (config_key, config_value) VALUES 
('WORK_START_TIME', '09:00'),
('GRACE_PERIOD_MINS', '15'),
('TELEGRAM_BOT_TOKEN', 'YOUR_BOT_TOKEN_HERE');

-- Insert default Admin user (Password: admin123)
-- Note: Password should be BCrypt encoded in real application. 
-- For demo purposes assume $2a$10$Something... here or handle via code.
-- Here we just put a placeholder. The App will likely rely on JPA to create tables if ddl-auto=update is used, 
-- but this script is good for reference or Flyway.
