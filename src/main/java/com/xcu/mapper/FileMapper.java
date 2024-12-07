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

}
