package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_file")
@Builder
public class FileInfo {

    @TableId(value = "file_id")
    private Long fileId;

    private String fileMd5;

    private Long fileSize;

    private String fileCover; // 文件封面（不一定都有）

    private String filePath; // 实际存储在服务器中的路径

    private Integer fileCategory; // 1:视频 2:音频 3:图片 4:文档 5:其他

    private Integer fileType; // 1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他

    private Integer status; // 0:转码中 1:转码失败 2:使用中

    private LocalDateTime createTime;

    private Integer refCount; // 引用次数

}