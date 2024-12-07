package com.xcu.controller;

import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.LoadDataListDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.entity.dto.UploadFileDTO;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.result.PageResult;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @PostMapping("/loadDataList")
    public Result<PageResult<LoadDataListVO>> loadDataList(LoadDataListDTO loadDataListDTO) {
        return fileService.loadDataList(loadDataListDTO);
    }

    @PostMapping("/uploadFile")
    public Result uploadFile(UploadFileDTO uploadFileDTO) {
        return fileService.uploadFile(uploadFileDTO);
    }

    @GetMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response,
                         @PathVariable("imageFolder") String imageFolder,
                         @PathVariable("imageName") String imageName) {
        fileService.getImage(response, imageFolder, imageName);
    }

    @PostMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response,
                        @PathVariable("fileId") Long fileId) {
        fileService.getFile(response, fileId);
    }

    @GetMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("fileId") String fileId) {
        fileService.getVideoInfo(response, fileId);
    }

    @PostMapping("/rename")
    public Result rename(Long fileId, String fileName) {
        return fileService.rename(fileId, fileName);
    }

    @PostMapping("/delFile")
    public Result delFile(String fileIds) {
        return fileService.delFile(fileIds);
    }

    @PostMapping("/createDownloadUrl/{fileId}")
    public Result createDownloadUrl(@PathVariable Long fileId) {
        return fileService.createDownloadUrl(fileId);
    }

    @GetMapping("/download/{code}")
    public void download(@PathVariable String code, HttpServletResponse response) throws IOException {
        fileService.download(code, response);
    }

    @PostMapping("/newFoloder")
    public Result newFolder(NewFolderDTO newFolderDTO) {
        return fileService.newFolder(newFolderDTO);
    }

    // 前端写错路径了。。
//    @PostMapping("/admin/getFolderInfo")
//    public Result getFolderInfo(String path) {
//        return fileService.getFolderInfo(path);
//    }

    @PostMapping("/loadAllFolder")
    public Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO) {
        return fileService.loadAllFolder(loadAllFolderDTO);
    }

    @PostMapping("/changeFileFolder")
    public Result changeFileFolder(String fileIds, Long filePid) {
        return fileService.changeFileFolder(fileIds, filePid);
    }

}
