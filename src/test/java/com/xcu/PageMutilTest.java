package com.xcu;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.mapper.FileFolderMapper;
import com.xcu.mapper.FileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PageMutilTest {

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private FileFolderMapper fileFolderMapper;

    @Test
    public void test() {
        IPage<LoadDataListVO> page = new Page<>(0, 15);
        page = fileFolderMapper.selectFileInfoPage(page, null, 0L, null);

        page.getRecords().forEach(System.out::println);
    }

    @Test
    public void test2() {
        System.out.println(1099511627776L / 1024L);
    }

}
