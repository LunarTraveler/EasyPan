package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcu.entity.pojo.Share;
import com.xcu.entity.vo.GetShareLoginInfoVO;
import com.xcu.entity.vo.LoadShareListVO;

public interface ShareMapper extends BaseMapper<Share> {

    IPage<LoadShareListVO> loadShareList(IPage<LoadShareListVO> page, Long userId);


    GetShareLoginInfoVO getShareLoginInfo(Long shareId);

    void updateShowIncrement(Long shareId);
}
