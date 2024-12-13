package com.xcu.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetShareLoginInfoVO {

    private LocalDateTime shareTime;

    private LocalDateTime expireTime;

    private String nickName;

    private String fileName;

    private Boolean currentUser;

    private Long fileId;

    private String avatar;

    private Long userId;

}
