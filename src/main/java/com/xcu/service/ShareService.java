package com.xcu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcu.entity.dto.LoadFileListDTO;
import com.xcu.entity.dto.SaveShareDTO;
import com.xcu.entity.dto.ShareFileDTO;
import com.xcu.entity.pojo.Share;
import com.xcu.result.Result;
import org.springframework.stereotype.Service;

public interface ShareService extends IService<Share> {

    Result loadShareList(Integer pageNo, Integer pageSize);

    Result shareFile(ShareFileDTO shareFileDTO);

    Result cancelShare(String[] ids);

    Result getShareLoginInfo(Long shareId);

    Result getShareInfo(Long shareId);

    Result checkShareCode(Long shareId, String code);

    Result loadFileList(LoadFileListDTO loadFileListDTO);

    Result getFolderInfo(Long shareId, String path);

    Result saveShare(SaveShareDTO saveShareDTO);
}
