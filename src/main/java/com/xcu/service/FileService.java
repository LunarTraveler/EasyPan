package com.xcu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.LoadDataListDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.entity.dto.UploadFileDTO;
import com.xcu.entity.pojo.FileInfo;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.result.PageResult;
import com.xcu.result.Result;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface FileService extends IService<FileInfo> {

    Result<PageResult<LoadDataListVO>> loadDataList(LoadDataListDTO loadDataListDTO);

    Result uploadFile(UploadFileDTO uploadFileDTO);

    void unionFile(String tempFileFolder, String targetFolder, String targetFileName, int chunks, long fileId) throws Exception;

    void getImage(HttpServletResponse response, String imageFolder, String imageName);

    void getFile(HttpServletResponse response, Long fileId);

    void getVideoInfo(HttpServletResponse response, String fileId);

    Result rename(Long fileId, String fileName);

    Result delFile(String fileIds);

    Result createDownloadUrl(Long fileId);

    void download(String code, HttpServletResponse response) throws IOException;

    Result newFolder(NewFolderDTO newFolderDTO);

    Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO);

    Result changeFileFolder(String fileIds, Long filePid);
}
