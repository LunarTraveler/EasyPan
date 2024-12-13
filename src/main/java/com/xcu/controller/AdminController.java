package com.xcu.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcu.constants.RedisConstant;
import com.xcu.entity.dto.LoadFileListDTO;
import com.xcu.entity.dto.LoadUserListDTO;
import com.xcu.entity.dto.SaveSysSettingsDTO;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final UserService userService;

    private final FileService fileService;

    @PostMapping("/saveSysSettings")
    public Result saveSysSettings(SaveSysSettingsDTO saveSysSettingsDTO) {
        try {
            String settingJson = objectMapper.writeValueAsString(saveSysSettingsDTO);
            stringRedisTemplate.opsForValue().set(RedisConstant.SystemSetting_KEY, settingJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Result.success();
    }

    @PostMapping("/getSysSettings")
    public Result getSysSettings() {
        String settingJson = stringRedisTemplate.opsForValue().get(RedisConstant.SystemSetting_KEY);
        try {
            SaveSysSettingsDTO SystemSetting = objectMapper.readValue(settingJson, SaveSysSettingsDTO.class);
            return Result.success(SystemSetting);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/loadUserList")
    public Result loadUserList(LoadUserListDTO loadUserListDTO) {
        Integer pageNo = loadUserListDTO.getPageNo();
        Integer pageSize = loadUserListDTO.getPageSize();
        loadUserListDTO.setPageNo(pageNo == null ? 1 : pageNo);
        loadUserListDTO.setPageSize(pageSize == null ? 15 : pageSize);

        return userService.loadUserList(loadUserListDTO);
    }

    @PostMapping("/updateUserStatus")
    public Result updateUserStatus(Long userId, Boolean status) {
        return userService.updateUserStatus(userId, status);
    }

    @PostMapping("/updateUserSpace")
    public Result updateUserSpace(Long userId, Long changeSpace) {
        return userService.updateUserSpace(userId, changeSpace);
    }

    @PostMapping("/loadFileList")
    public Result loadFileList(LoadFileListDTO loadFileListDTO) {
        Integer pageNo = loadFileListDTO.getPageNo();
        Integer pageSize = loadFileListDTO.getPageSize();
        loadFileListDTO.setPageNo(pageNo == null ? 1 : pageNo);
        loadFileListDTO.setPageSize(pageSize == null ? 15 : pageSize);

        return fileService.loadFileList(loadFileListDTO);
    }

    @PostMapping("/getFolderInfo")
    public Result getFolderInfo(String path) {
        return fileService.getFolderInfo(path);
    }

    /**
     * 这里其实就是文件的预览
     * @param response
     * @param userId
     * @param fileId
     * @return
     */
    @PostMapping("/getFile/{userId}/{fileId}")
    public void getFile(HttpServletResponse response,
                          @PathVariable("userId") String userId,
                          @PathVariable("fileId") Long fileId) {
        fileService.getFile(response, fileId);
    }

    @GetMapping("/ts/getVideoInfo/{userId}/{fileId}")
    public void getVideoInfo(HttpServletResponse response,
                             @PathVariable("userId") String userId,
                             @PathVariable("fileId") String fileId) {
        fileService.getVideoInfo(response, fileId);
    }

    @PostMapping("/createDownloadUrl/{userId}/{fileId}")
    public Result createDownloadUrl(@PathVariable("userId") String userId,
                                    @PathVariable("fileId") Long fileId) {
        return fileService.createDownloadUrl(fileId);
    }

    @GetMapping("/download/{fileId}")
    public void download(HttpServletResponse response,
                         @PathVariable("fileId") Long fileId) throws IOException {
        fileService.adminDownload(response, fileId);
    }

    @PostMapping("/delFile")
    public Result delFile(String fileIds) {
        // userId_fileId
        return fileService.delFile(fileIds);
    }

}
