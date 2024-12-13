package com.xcu.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcu.entity.vo.LoadShareListVO;
import com.xcu.result.PageResult;

import java.util.ArrayList;
import java.util.Collections;

public class PageResultConversionUtil {

    /**
     * 用于结果的变换
     * @param page
     * @return
     * @param <T>
     */
    public static <T> PageResult<T> conversion(IPage page, Class<T> clazz) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPageNo((int)page.getCurrent());
        pageResult.setPageSize((int)page.getSize());
        pageResult.setTotalCount(page.getTotal());
        pageResult.setPageTotal(page.getPages());
        pageResult.setList(new ArrayList<>());

        return pageResult;
    }

}
