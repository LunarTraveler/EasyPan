package com.xcu.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private Long totalCount;
    private Integer pageSize;
    private Integer pageNo;
    private Long pageTotal;
    private List<T> list;

}
