-- 创建数据库
CREATE DATABASE easypan
    CHARACTER SET utf8mb4
    COLLATE utf8mb4;

-- 创建表
CREATE TABLE `easypan`.`tb_user`
(
    `user_id`         bigint                                                        NOT NULL COMMENT '用户表主键id',
    `nick_name`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL     DEFAULT NULL COMMENT '用户昵称',
    `email`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL     DEFAULT NULL COMMENT '邮箱',
    `qq_open_id`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL     DEFAULT NULL COMMENT 'qq获取的open_id',
    `qq_avatar`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL     DEFAULT NULL COMMENT 'qq头像',
    `password`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NULL     DEFAULT NULL COMMENT 'md5加密后的值',
    `status`          tinyint(1)                                                    NULL     DEFAULT 1 COMMENT '0:禁用  1:启用',
    `used_space`      bigint                                                        NULL     DEFAULT NULL COMMENT '已经使用过的空间',
    `total_space`     bigint                                                        NULL     DEFAULT NULL COMMENT '总的空间',
    `last_login_time` datetime                                                      NULL     DEFAULT NULL COMMENT '最后一次使用的时间',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`user_id`) USING BTREE,
    UNIQUE INDEX `unique_nick_name` (`nick_name` ASC) USING BTREE,
    UNIQUE INDEX `unique_email` (`email` ASC) USING BTREE,
    UNIQUE INDEX `unique_qq_open_id` (`qq_open_id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '用户表'
  ROW_FORMAT = Dynamic;

CREATE TABLE `easypan`.`tb_file`
(
    `file_id`       bigint                                                        NOT NULL COMMENT '文件主键',
    `file_md5`      varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '文件的MD5值唯一标识',
    `file_size`     bigint                                                        NULL     DEFAULT NULL COMMENT '文件大小，为了标识精确采用B为单位',
    `file_path`     varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '实际存储位置',
    `file_cover`    varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '文件封面路径',
    `status`        tinyint                                                       NULL     DEFAULT NULL COMMENT '0：转码中 1：转码失败 2：转码成功',
    `ref_count`     int                                                           NULL     DEFAULT NULL COMMENT '引用次数',
    `create_time`   datetime                                                      NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '文件上传时间',
    `file_category` tinyint                                                       NULL     DEFAULT NULL COMMENT '文件分类 1：视频 2：音频 3：图片 4：文档 5：其他',
    `file_type`     tinyint                                                       NULL     DEFAULT NULL COMMENT '1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他',
    `update_time`   datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

CREATE TABLE `easypan`.`tb_file_folder`
(
    `id`             bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '主键的表示',
    `user_id`        bigint                                                        NOT NULL COMMENT '表示所属的用户',
    `file_folder_id` bigint                                                        NULL DEFAULT NULL COMMENT '对应的文件id',
    `name`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
    `is_directory`   tinyint(1)                                                    NULL DEFAULT NULL COMMENT '0文件  1目录',
    `parent_id`      bigint                                                        NULL DEFAULT NULL COMMENT '根目录默认是0',
    `status`         tinyint                                                       NULL DEFAULT NULL COMMENT '0：删除 1：回收站 2：正常存在',
    `recovery_time`  datetime                                                      NULL DEFAULT NULL COMMENT '回收时间',
    `create_time`    datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 23
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  ROW_FORMAT = Dynamic;

CREATE TABLE `easypan`.`tb_share`
(
    `share_id`    bigint                                                      NOT NULL COMMENT '分享表的主键',
    `file_id`     bigint                                                      NOT NULL COMMENT '分享文件的主键',
    `user_id`     bigint                                                      NOT NULL COMMENT '分享人的信息',
    `valid_type`  tinyint                                                     NULL DEFAULT NULL COMMENT '有效期类型 0：1天 1：7天 2：30天 3：永久有效',
    `expire_time` datetime                                                    NULL DEFAULT NULL COMMENT '过期时间',
    `share_time`  datetime                                                    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
    `code`        varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分享码 字长为5',
    `show_count`  int                                                         NULL DEFAULT 0 COMMENT '分享次数或是下载次数',
    PRIMARY KEY (`share_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = Dynamic;







