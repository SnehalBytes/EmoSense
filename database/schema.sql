-- ==========================================================
-- EmoSense Database Schema Initialization
-- Database: emosense
-- ==========================================================

CREATE DATABASE IF NOT EXISTS emosense
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE emosense;

-- ----------------------------------------------------------
-- 1. Users Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- 2. Check-Ins Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS check_ins (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    has_photo BOOLEAN NOT NULL DEFAULT FALSE,
    has_text BOOLEAN NOT NULL DEFAULT FALSE,
    photo_file_name VARCHAR(255),
    thoughts_text TEXT,
    analysis_mode VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_check_ins_user_created (user_id, created_at DESC)
) ENGINE=InnoDB;

-- ----------------------------------------------------------
-- 3. Analysis Results Table
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS analysis_results (
    id VARCHAR(64) PRIMARY KEY,
    check_in_id VARCHAR(64) NOT NULL,
    primary_signal VARCHAR(100),
    confidence DOUBLE,
    photo_signal VARCHAR(100),
    text_signal VARCHAR(100),
    combined_signal VARCHAR(100),
    signal_agreement VARCHAR(100),
    model_source VARCHAR(50) DEFAULT 'REAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (check_in_id) REFERENCES check_ins(id) ON DELETE CASCADE,
    INDEX idx_analysis_results_check_in (check_in_id)
) ENGINE=InnoDB;
