package com.xcu.entity.dto;

import lombok.Data;

@Data
public class SaveSysSettingsDTO {

    private String registerEmailTitle;

    private String registerEmailContent;

    // 这个是定义用户初始化的使用空间，可以动态改变的（如果是静态定义的缺少了可变性）
    private Long userInitUseSpace;

}
