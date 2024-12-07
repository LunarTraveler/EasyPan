package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "tb_file_folder")
@Builder
public class FileFolder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long fileFolderId;

    private String name;

    private Integer isDirectory; // 0文件  1目录

    private Long parentId;

    private Integer status; // 0：删除 1：回收站 2：正常存在

    private LocalDateTime recoveryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
