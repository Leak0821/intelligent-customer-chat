CREATE DATABASE IF NOT EXISTS `xxl_job`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `xxl_job`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_group` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `app_name` VARCHAR(64) NOT NULL COMMENT '执行器 AppName',
    `title` VARCHAR(64) NOT NULL COMMENT '执行器名称',
    `address_type` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list` TEXT DEFAULT NULL COMMENT '执行器地址列表，多地址逗号分隔',
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_registry` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `registry_group` VARCHAR(50) NOT NULL,
    `registry_key` VARCHAR(255) NOT NULL,
    `registry_value` VARCHAR(255) NOT NULL,
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_info` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `job_group` INT(11) NOT NULL COMMENT '执行器主键 ID',
    `job_desc` VARCHAR(255) NOT NULL,
    `add_time` DATETIME DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    `author` VARCHAR(64) DEFAULT NULL COMMENT '作者',
    `alarm_email` VARCHAR(255) DEFAULT NULL COMMENT '报警邮箱',
    `schedule_type` VARCHAR(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
    `schedule_conf` VARCHAR(128) DEFAULT NULL COMMENT '调度配置',
    `misfire_strategy` VARCHAR(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    `executor_route_strategy` VARCHAR(50) DEFAULT NULL COMMENT '执行器路由策略',
    `executor_handler` VARCHAR(255) DEFAULT NULL COMMENT '任务 handler',
    `executor_param` TEXT DEFAULT NULL COMMENT '任务参数',
    `executor_block_strategy` VARCHAR(50) DEFAULT NULL COMMENT '阻塞处理策略',
    `executor_timeout` INT(11) NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` INT(11) NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `glue_type` VARCHAR(50) NOT NULL COMMENT 'GLUE 类型',
    `glue_source` MEDIUMTEXT DEFAULT NULL COMMENT 'GLUE 源代码',
    `glue_remark` VARCHAR(128) DEFAULT NULL COMMENT 'GLUE 备注',
    `glue_updatetime` DATETIME DEFAULT NULL COMMENT 'GLUE 更新时间',
    `child_jobid` VARCHAR(255) DEFAULT NULL COMMENT '子任务 ID，多个逗号分隔',
    `trigger_status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time` BIGINT(13) NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time` BIGINT(13) NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_logglue` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `job_id` INT(11) NOT NULL COMMENT '任务主键 ID',
    `glue_type` VARCHAR(50) DEFAULT NULL COMMENT 'GLUE 类型',
    `glue_source` MEDIUMTEXT DEFAULT NULL COMMENT 'GLUE 源代码',
    `glue_remark` VARCHAR(128) NOT NULL COMMENT 'GLUE 备注',
    `add_time` DATETIME DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
    `job_group` INT(11) NOT NULL COMMENT '执行器主键 ID',
    `job_id` INT(11) NOT NULL COMMENT '任务主键 ID',
    `executor_address` VARCHAR(255) DEFAULT NULL COMMENT '执行器地址',
    `executor_handler` VARCHAR(255) DEFAULT NULL COMMENT '任务 handler',
    `executor_param` TEXT DEFAULT NULL COMMENT '任务参数',
    `executor_sharding_param` VARCHAR(20) DEFAULT NULL COMMENT '任务分片参数',
    `executor_fail_retry_count` INT(11) NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `trigger_time` DATETIME DEFAULT NULL COMMENT '调度时间',
    `trigger_code` INT(11) NOT NULL COMMENT '调度结果',
    `trigger_msg` TEXT DEFAULT NULL COMMENT '调度日志',
    `handle_time` DATETIME DEFAULT NULL COMMENT '执行时间',
    `handle_code` INT(11) NOT NULL COMMENT '执行状态',
    `handle_msg` TEXT DEFAULT NULL COMMENT '执行日志',
    `alarm_status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY `I_trigger_time` (`trigger_time`),
    KEY `I_handle_code` (`handle_code`),
    KEY `I_jobgroup` (`job_group`),
    KEY `I_jobid` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_log_report` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `trigger_day` DATETIME DEFAULT NULL COMMENT '调度日期',
    `running_count` INT(11) NOT NULL DEFAULT '0' COMMENT '运行中日志数量',
    `suc_count` INT(11) NOT NULL DEFAULT '0' COMMENT '执行成功日志数量',
    `fail_count` INT(11) NOT NULL DEFAULT '0' COMMENT '执行失败日志数量',
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_lock` (
    `lock_name` VARCHAR(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_user` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '账号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码加密信息',
    `token` VARCHAR(100) DEFAULT NULL COMMENT '登录 token',
    `role` TINYINT(4) NOT NULL COMMENT '角色：0-普通用户、1-管理员',
    `permission` VARCHAR(255) DEFAULT NULL COMMENT '权限：执行器 ID 列表，多个逗号分隔',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `xxl_job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES (1, 'intelligent-customer-chat-executor', '智能客服邮件执行器', 0, NULL, NOW())
ON DUPLICATE KEY UPDATE
    `app_name` = VALUES(`app_name`),
    `title` = VALUES(`title`),
    `address_type` = VALUES(`address_type`),
    `address_list` = VALUES(`address_list`),
    `update_time` = VALUES(`update_time`);

INSERT INTO `xxl_job_user` (`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `permission` = VALUES(`permission`);

INSERT INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock')
ON DUPLICATE KEY UPDATE
    `lock_name` = VALUES(`lock_name`);
