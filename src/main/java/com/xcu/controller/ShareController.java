package com.xcu.controller;

import cn.hutool.core.lang.UUID;
import com.xcu.entity.dto.ShareFileDTO;
import com.xcu.result.Result;
import com.xcu.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Random;

@RestController
@RequiredArgsConstructor
@RequestMapping("/share")
@Slf4j
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/loadShareList")
    public Result loadShareList(Integer pageNo, Integer pageSize) {
        pageNo = pageNo == null ? 1 : pageNo;
        pageSize = pageSize == null ? 15 : pageSize;
        return shareService.loadShareList(pageNo, pageSize);
    }

    @PostMapping("/shareFile")
    public Result shareFile(ShareFileDTO shareFileDTO) {
        if (shareFileDTO.getCode() == null) {
            String code = UUID.fastUUID().toString().substring(0, 5);
            shareFileDTO.setCode(code);
        }
        return shareService.shareFile(shareFileDTO);
    }

    @PostMapping("/cancelShare")
    public Result cancelShare(String shareIds) {
        String[] ids = shareIds.split(",");
        return shareService.cancelShare(ids);
    }



}
