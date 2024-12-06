package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user_file")
@Builder
public class UserWithFile {

    @TableId(value = "id")
    private Long id;

    private Long userId;

    private Long fileId;

    private String fileName;

    private Long filePid; // 用户生成的目录id

    private Integer status; // 0：删除 1：回收站 2：正常存在

    private LocalDateTime recoveryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
