package com.xcu.entity.dto;

import lombok.Data;

@Data
public class LoginDTO {

    private String email;

    private String password;

    private String checkCode;

}
