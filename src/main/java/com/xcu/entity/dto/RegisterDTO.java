package com.xcu.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegisterDTO {

    private String email;

    private String nickName;

    private String password;

    private String checkCode;

    private String emailCode;

}
