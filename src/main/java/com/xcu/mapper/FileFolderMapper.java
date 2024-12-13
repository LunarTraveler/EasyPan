package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
                                             @Param("fileId") Long fileId,
                                             @Param("fileName") String fileName,
                                             @Param("userId") Long userId,
                                             @Param("isDirectory") Integer isDirectory);

    void updateBatchFolderId(Long userId, String[] fileId, Long filePid);

    List<GetFolderInfo> getFolderInfo(String[] folderIds, Long userId);

    List<Long> getFirstMatchingNodeEncountered(Long userId);

    IPage<LoadDataListVO> loadRecycleList(IPage page, List<Long> ids);

    void recursiveFileInRecovery(String[] idArray);

    void recursiveFileOutRecovery(String[] fileFolders);

    void recursiveCompleteDelFile(String[] fileFolders);

    void insertBatchs(List<FileFolder> fileFolders);
}
