package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
@Builder
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id")
    private Long id;

    private String nickName;

    private String email;

    // qq授权码
    private String qqOpenId;

    private String qqAvatar;

    private String password;

    private Boolean status = true; // 0 禁用   1 启用

    private Long usedSpace;

    private Long totalSpace;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
