package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "tb_folders")
public class Folder {

    @TableId(value = "folder_id", type = IdType.AUTO)
    private Long folderId; // 自增的 逻辑0表示为根目录

    private Long userId;

    private Long parentId;

    private String name;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
