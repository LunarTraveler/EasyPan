package com.xcu.entity.dto;

import lombok.Data;

@Data
public class ResetPwdDTO {

    private String email;

    private String password;

    private String checkCode;

    private String emailCode;

}
