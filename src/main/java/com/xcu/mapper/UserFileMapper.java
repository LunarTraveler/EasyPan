package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcu.entity.pojo.User;
import com.xcu.entity.pojo.UserWithFile;
import com.xcu.entity.vo.LoadDataListVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserFileMapper extends BaseMapper<UserWithFile> {

    void updateBatchFolderId(Long userId, String[] fileId, Long filePid);


    void updateBatchStatus(List<Long> fileIds, Long userId, Integer status, LocalDateTime recoveryTime);
}
