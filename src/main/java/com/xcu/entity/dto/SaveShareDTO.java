package com.xcu.entity.dto;

import lombok.Data;

@Data
public class SaveShareDTO {

    private Long shareId;

    private String shareFileIds; // 分享的文件ID,多个逗号隔开

    private Long myFolderId; // 我的网盘目录ID

}
