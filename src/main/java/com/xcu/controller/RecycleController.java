package com.xcu.controller;

import com.xcu.result.Result;
import com.xcu.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/recycle")
public class RecycleController {

    private final FileService fileService;

    @PostMapping("/loadRecycleList")
    public Result loadRecycleList(Integer pageNo, Integer pageSize) {
        pageNo = pageNo == null ? 1 : pageNo;
        pageSize = pageSize == null ? 15 : pageSize;
        return fileService.loadRecycleList(pageNo, pageSize);
    }

    @PostMapping("/recoverFile")
    public Result recoverFile(String filIds) {
        String[] fileFolders = filIds.split(",");
        return fileService.recoverFile(fileFolders);
    }

    @PostMapping("/delFile")
    public Result delFile(String filIds) {
        String[] fileFolders = filIds.split(",");
        return fileService.completeDelFile(fileFolders);
    }

}
