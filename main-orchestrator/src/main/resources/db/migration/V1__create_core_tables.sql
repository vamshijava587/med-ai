CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(255),
    allergies TEXT,
    medications TEXT,
    insurance_provider VARCHAR(255),
    insurance_plan VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    token_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_messages_session_created (session_id, created_at),
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);

CREATE TABLE IF NOT EXISTS tool_calls (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    agent_name VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    tool_input LONGTEXT,
    tool_output LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tool_calls_session_created (session_id, created_at)
);

CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    agent_name VARCHAR(64) NOT NULL,
    confidence_score DOUBLE NOT NULL,
    relevance_score DOUBLE NOT NULL,
    hallucination_flag BOOLEAN NOT NULL,
    rationale LONGTEXT,
    validation_notes JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_evaluations_session_created (session_id, created_at)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    query_text LONGTEXT NOT NULL,
    agent_selected VARCHAR(64) NOT NULL,
    tools_used JSON,
    tokens_used INT NOT NULL,
    confidence_score DOUBLE NOT NULL,
    hallucination_flag BOOLEAN NOT NULL,
    response_time_ms BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_logs_user_created (user_id, created_at),
    INDEX idx_audit_logs_session_created (session_id, created_at)
);

INSERT INTO users(user_id, full_name, allergies, medications, insurance_provider, insurance_plan, notes)
VALUES ('demo-user', 'Demo User', 'penicillin, pollen', 'cetirizine', 'Acme Health', 'Silver PPO', 'Seed user for local development')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;
