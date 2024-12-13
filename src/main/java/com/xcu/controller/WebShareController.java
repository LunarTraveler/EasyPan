package com.xcu.controller;

import com.xcu.entity.dto.LoadFileListDTO;
import com.xcu.entity.dto.SaveShareDTO;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.service.ShareService;
import com.xcu.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/showShare")
@Slf4j
public class WebShareController {

    private final ShareService shareService;

    private final UserService userService;

    private final FileService fileService;

    @PostMapping("/getShareLoginInfo")
    public Result getShareLoginInfo(Long shareId) {
        return shareService.getShareLoginInfo(shareId);
    }

    @PostMapping("/getShareInfo")
    public Result getShareInfo(Long shareId) {
        return shareService.getShareInfo(shareId);
    }

    @PostMapping("/checkShareCode")
    public Result checkShareCode(Long shareId, String code) {
        return shareService.checkShareCode(shareId, code);
    }

    @PostMapping("/loadFileList")
    public Result loadFileList(LoadFileListDTO loadFileListDTO) {
        Integer pageNo = loadFileListDTO.getPageNo();
        Integer pageSize = loadFileListDTO.getPageSize();
        loadFileListDTO.setPageNo(pageNo == null ? 1 : pageNo);
        loadFileListDTO.setPageSize(pageSize == null ? 15 : pageSize);

        return shareService.loadFileList(loadFileListDTO);
    }

    @PostMapping("/getFolderInfo")
    public Result getFolderInfo(Long shareId, String path) {
        return shareService.getFolderInfo(shareId, path);
    }

    /**
     * 这里其实就是文件的预览
     * @param response
     * @param fileId
     * @return
     */
    @PostMapping("/getFile/{shareId}/{fileId}")
    public void getFile(HttpServletResponse response,
                        @PathVariable("shareId") String shareId,
                        @PathVariable("fileId") Long fileId) {
        fileService.getFile(response, fileId);
    }

    @GetMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("shareId") String shareId,
                             @PathVariable("fileId") String fileId) {
        fileService.getVideoInfo(response, fileId);
    }

    @PostMapping("/createDownloadUrl/{shareId}/{fileId}")
    public Result createDownloadUrl(@PathVariable("shareId") String shareId,
                                    @PathVariable("fileId") Long fileId) {
        return fileService.createDownloadUrl(fileId);
    }

    @GetMapping("/download/{code}")
    public void download(HttpServletResponse response,
                         @PathVariable("code") String code) throws IOException {
        fileService.download(code, response);
    }

    @PostMapping("/saveShare")
    public Result saveShare(SaveShareDTO saveShareDTO) {
        return shareService.saveShare(saveShareDTO);
    }

}
