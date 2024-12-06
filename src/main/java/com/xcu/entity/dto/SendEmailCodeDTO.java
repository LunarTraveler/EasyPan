package com.xcu.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SendEmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "图形验证码不能为空")
    private String checkCode;

    @NotNull(message = "默认为注册")
    private Integer type = 0; // 0:注册 1:找回密码 默认为0

}
