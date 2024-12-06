-- 创建数据库
CREATE DATABASE easypan
CHARACTER SET utf8mb4
COLLATE utf8mb4;

-- 创建表
CREATE TABLE user_info (
   `user_id` BIGINT NOT NULL COMMENT '用户表主键id',
   `nick_name` VARCHAR(255) NULL COMMENT '用户昵称',
   `email` VARCHAR(255) NULL COMMENT '邮箱',
   `qq_open_id` VARCHAR(255) NULL COMMENT 'qq获取的open_id',
   `qq_avatar` VARCHAR(255) NULL COMMENT 'qq头像',
   `password` VARCHAR(64) NULL COMMENT 'md5加密后的值',
   `status` TINYINT(1) NULL DEFAULT 1 COMMENT '0:禁用  1:启用',
   `used_space` INT NULL COMMENT '已经使用过的空间',
   `total_space` INT NULL COMMENT '总的空间',
   `last_login_time` DATETIME NULL COMMENT '最后一次使用的时间',
   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
   PRIMARY KEY (`user_id`),
   UNIQUE INDEX `unique_nick_name` (`nick_name`),
   UNIQUE INDEX `unique_email` (`email`),
   UNIQUE INDEX `unique_qq_open_id` (`qq_open_id`)
) COMMENT = '用户表';





