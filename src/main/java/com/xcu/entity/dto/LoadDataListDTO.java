package com.xcu.entity.dto;

import lombok.Data;

@Data
public class LoadDataListDTO {

    private String category;

    private Long filePid;

    private String fileNameFuzzy;

    private Integer pageNo;

    private Integer pageSize;

}
