package com.xcu.entity.dto;

import lombok.Data;

@Data
public class LoadUserListDTO {

    private Integer pageNo;

    private Integer pageSize;

    private String nickNameFuzzy; // 名称模糊匹配用的(一般都是左匹配或是右匹配，可以利用索引加速)

    private Integer status; // 启用还是禁用

}
