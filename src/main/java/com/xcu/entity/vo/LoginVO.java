package com.xcu.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {

    private String nickName;

    private String userId;

    private String avatar;

    private Boolean admin;

}
