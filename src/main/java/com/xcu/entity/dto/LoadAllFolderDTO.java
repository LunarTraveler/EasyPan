package com.xcu.entity.dto;

import lombok.Data;

@Data
public class LoadAllFolderDTO {

    private Long filePid; // 父目录id

    private String currentFileIds; // 当前文件或是目录id，可能会有多个值用逗号隔开

}
