package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcu.entity.pojo.FileFolder;
import com.xcu.entity.vo.GetFolderInfo;
import com.xcu.entity.vo.LoadDataListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileFolderMapper extends BaseMapper<FileFolder> {

    // 使用 Page 对象作为返回类型
    IPage<LoadDataListVO> selectFileInfoPage(IPage page,
                                             @Param("category") Integer category,
                                             @Param("filePid") Long filePid,
                                             @Param("fileName") String fileName);

    List<GetFolderInfo> selectFolderInfoList(String[] folderIds);

    void updateBatchFolderId(Long userId, String[] fileId, Long filePid);

    void recursiveRecovery(String[] idArray);
}
