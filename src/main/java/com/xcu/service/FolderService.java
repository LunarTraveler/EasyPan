package com.xcu.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.entity.pojo.Folder;
import com.xcu.result.Result;

public interface FolderService extends IService<Folder> {

    Result newFolder(NewFolderDTO newFolderDTO);

    Result getFolderInfo(String path);

    Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO);

    Result changeFileFolder(String fileIds, Long filePid);
}
