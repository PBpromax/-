-- Keep this copy aligned with /mysql-init/init.sql for local backend-oriented setup.
CREATE DATABASE IF NOT EXISTS `campushub_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `campushub_db`;

DROP TABLE IF EXISTS `biz_evaluation`;
DROP TABLE IF EXISTS `biz_notification`;
DROP TABLE IF EXISTS `biz_order`;
DROP TABLE IF EXISTS `biz_requirement`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `user_id` BIGINT NOT NULL COMMENT '用户唯一标识，雪花算法生成',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '加盐哈希密码',
  `phone_encrypted` VARCHAR(255) NOT NULL COMMENT '加密后的手机号',
  `student_id` VARCHAR(32) NOT NULL COMMENT '学号',
  `campus` VARCHAR(64) DEFAULT NULL COMMENT '校区',
  `college` VARCHAR(64) DEFAULT NULL COMMENT '学院',
  `major` VARCHAR(64) DEFAULT NULL COMMENT '专业',
  `grade` VARCHAR(32) DEFAULT NULL COMMENT '年级',
  `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
  `contact_visible` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否展示联系方式',
  `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用积分，初始100',
  `role` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '系统角色：0普通用户，1管理员',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表';

CREATE TABLE `biz_requirement` (
  `req_id` BIGINT NOT NULL COMMENT '需求唯一标识',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者用户ID',
  `title` VARCHAR(128) NOT NULL COMMENT '需求标题',
  `description` TEXT NOT NULL COMMENT '需求详情描述',
  `budget` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '预算金额，精确到分',
  `type` VARCHAR(32) NOT NULL COMMENT '需求分类枚举值',
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '需求状态：PENDING, ACCEPTED, COMPLETED, CANCELED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`req_id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='需求信息表';

CREATE TABLE `biz_order` (
  `order_id` BIGINT NOT NULL COMMENT '订单唯一标识',
  `req_id` BIGINT NOT NULL COMMENT '关联的需求ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接单者用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '交易最终金额',
  `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '订单状态：IN_PROGRESS, TO_CONFIRM, COMPLETED, CANCELED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接单/创建时间',
  `finished_at` DATETIME DEFAULT NULL COMMENT '订单完成时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_req_id` (`req_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单表';

CREATE TABLE `biz_evaluation` (
  `eval_id` BIGINT NOT NULL COMMENT '评价唯一标识',
  `order_id` BIGINT NOT NULL COMMENT '关联的订单ID',
  `reviewer_id` BIGINT NOT NULL COMMENT '评价方用户ID',
  `target_id` BIGINT NOT NULL COMMENT '被评价方用户ID',
  `star` TINYINT NOT NULL COMMENT '星级评分(1-5)',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '文字评价内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`eval_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单评价表';

CREATE TABLE `biz_notification` (
  `notification_id` BIGINT NOT NULL COMMENT '通知唯一标识',
  `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
  `title` VARCHAR(128) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(500) NOT NULL COMMENT '通知正文',
  `event_type` VARCHAR(64) NOT NULL COMMENT '触发事件类型',
  `read_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
  PRIMARY KEY (`notification_id`),
  KEY `idx_user_read_created` (`user_id`, `read_status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知表';
