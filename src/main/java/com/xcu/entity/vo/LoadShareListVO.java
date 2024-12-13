package com.xcu.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadShareListVO {

    private Long shareId;

    private Long fileId;

    private Long userId;

    private Integer validType;

    private LocalDateTime expireTime;

    private LocalDateTime shareTime;

    private String code;

    private Integer showCount;

    private String fileName;

    private Integer folderType;

    private Integer fileCategory;

    private Integer fileType;

    private String fileCover;

}
