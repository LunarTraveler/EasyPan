package com.xcu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcu.entity.pojo.Folder;
import com.xcu.entity.vo.GetFolderInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FolderMapper extends BaseMapper<Folder> {

    List<GetFolderInfo> selectFolderInfoList(String[] folderIds);

    /**
     * 递归查询出folderId这个目录下的所有文件和目录
     * @param folderId
     */
    List<Long> recursiveQuery(Long folderId);
}
