package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcu.entity.pojo.FileInfo;
import com.xcu.entity.pojo.User;
import com.xcu.entity.vo.LoadDataListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {

    Integer selectUserSpace(Long userId);

    Integer updateRefCount(String fileMd5);

    // 使用 Page 对象作为返回类型
    IPage<LoadDataListVO> selectFileInfoPage(IPage page,
                                                  @Param("category") String category,
                                                  @Param("filePid") Long filePid,
                                                  @Param("fileName") String fileName);

}
