package com.xcu.entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadFileDTO {

    private String fileId;

    private MultipartFile file;

    private String fileName;

    private String filePid;

    private String fileMd5;

    private Integer chunkIndex;

    private Integer chunks;

}
