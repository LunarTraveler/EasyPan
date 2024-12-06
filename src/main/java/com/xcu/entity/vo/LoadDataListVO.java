package com.xcu.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadDataListVO {

    private Long fileId;

    private Long filePid;

    private Long fileSize;

    private String fileName;

    private String fileCover;

    private Integer folderType; // 0: 文件 1: 目录

    private Integer fileCategory; // 1:视频 2:音频 3:图片 4:文档 5:其他

    private Integer fileType; // 1:视频 2:音频 3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他

    private Integer status; // 0:转码中 1:转码失败 2:转码成功

    private LocalDateTime createTime;

    private LocalDateTime lastUpdateTime;
}
