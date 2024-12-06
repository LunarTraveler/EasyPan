package com.xcu.controller;

import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.result.Result;
import com.xcu.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping("/file/newFoloder")
    public Result newFolder(NewFolderDTO newFolderDTO) {
        return folderService.newFolder(newFolderDTO);
    }

    // 前端写错路径了。。
    @PostMapping("/admin/getFolderInfo")
    public Result getFolderInfo(String path) {
        return folderService.getFolderInfo(path);
    }

    @PostMapping("/file/loadAllFolder")
    public Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO) {
        return folderService.loadAllFolder(loadAllFolderDTO);
    }

    @PostMapping("/file/changeFileFolder")
    public Result changeFileFolder(String fileIds, Long filePid) {
        return folderService.changeFileFolder(fileIds, filePid);
    }

}
