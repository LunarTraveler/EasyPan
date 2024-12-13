package com.xcu.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "tb_share")
@Builder
public class Share {

    @TableId(value = "share_id")
    private Long shareId;

    private Long fileId;

    private Long userId;

    private Integer validType;

    private LocalDateTime expireTime;

    private LocalDateTime shareTime;

    private String code;

    private Integer showCount;

}
