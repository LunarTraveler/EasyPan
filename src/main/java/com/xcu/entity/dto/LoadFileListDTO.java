package com.xcu.entity.dto;

import lombok.Data;

@Data
public class LoadFileListDTO {

    private Integer pageNo;

    private Integer pageSize;

    private String fileNameFuzzy; // 用于模糊匹配

    private Long shareId;

    private Long filePid;

}
