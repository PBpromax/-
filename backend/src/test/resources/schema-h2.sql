DROP TABLE IF EXISTS biz_evaluation;
DROP TABLE IF EXISTS biz_notification;
DROP TABLE IF EXISTS biz_order;
DROP TABLE IF EXISTS biz_requirement;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  user_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  nickname VARCHAR(64) DEFAULT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone_encrypted VARCHAR(255) NOT NULL,
  student_id VARCHAR(32) NOT NULL,
  campus VARCHAR(64) DEFAULT NULL,
  college VARCHAR(64) DEFAULT NULL,
  major VARCHAR(64) DEFAULT NULL,
  grade VARCHAR(32) DEFAULT NULL,
  bio VARCHAR(255) DEFAULT NULL,
  contact_visible TINYINT NOT NULL DEFAULT 0,
  credit_score INT NOT NULL DEFAULT 100,
  role TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_student_id (student_id)
);

CREATE TABLE biz_requirement (
  req_id BIGINT NOT NULL,
  publisher_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  description CLOB NOT NULL,
  budget DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (req_id),
  INDEX idx_publisher_id (publisher_id),
  INDEX idx_status_created (status, created_at)
);

CREATE TABLE biz_order (
  order_id BIGINT NOT NULL,
  req_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_req_id (req_id),
  INDEX idx_receiver_id (receiver_id),
  INDEX idx_status (status)
);

CREATE TABLE biz_evaluation (
  eval_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  star TINYINT NOT NULL,
  content VARCHAR(500) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (eval_id),
  INDEX idx_target_id (target_id),
  INDEX idx_order_id (order_id)
);

CREATE TABLE biz_notification (
  notification_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(500) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  read_status TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  INDEX idx_user_read_created (user_id, read_status, created_at)
);
