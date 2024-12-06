package com.xcu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcu.constants.Constants;
import com.xcu.context.BaseContext;
import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.entity.enums.ResponseCodeEnum;
import com.xcu.entity.pojo.Folder;
import com.xcu.entity.pojo.UserWithFile;
import com.xcu.entity.vo.GetFolderInfo;
import com.xcu.exception.BaseException;
import com.xcu.mapper.FolderMapper;
import com.xcu.mapper.UserFileMapper;
import com.xcu.result.Result;
import com.xcu.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder> implements FolderService {

    private final FolderMapper folderMapper;

    private final UserFileMapper userFileMapper;

    @Override
    public Result newFolder(NewFolderDTO newFolderDTO) {
        String name = newFolderDTO.getFileName();
        checkFolderName(name);

        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(newFolderDTO.getFilePid());
        folder.setUserId(BaseContext.getUserId());

        System.out.println(folder.getFolderId());
        folderMapper.insert(folder);
        System.out.println(folder.getFolderId());
        return Result.success();
    }

    /**
     * 这里要注意如果id的顺序没有大小之分的话，可以使用参数的先后顺序来确定位置
     * @param path 这个就是 a/b/c 这种的
     * @return
     */
    @Override
    public Result getFolderInfo(String path) {
        String[] folderIds = path.split("/");
        List<GetFolderInfo> getFolderInfos = folderMapper.selectFolderInfoList(folderIds);
        return Result.success(getFolderInfos);
    }

    /**
     * 检查目录名称是否重复
     * @param name
     */
    private void checkFolderName(String name) {
        Folder folder = folderMapper.selectOne(new LambdaQueryWrapper<Folder>().eq(Folder::getName, name));
        if (folder != null) {
            throw new BaseException("当前目录中已包含了这个目录名称，请在起一个新的名称");
        }
    }

    /**
     * 检查文件名称是否重复
     * @param name
     */
    private void checkFileName(String name) {
        Long userId = BaseContext.getUserId();
        UserWithFile userWithFile = userFileMapper.selectOne(new LambdaQueryWrapper<UserWithFile>()
                .eq(UserWithFile::getUserId, userId).eq(UserWithFile::getFileName, name));
        if (userWithFile != null) {
            throw new BaseException("此文件名已经存在，请在起一个新的名称");
        }

    }

    @Override
    public Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO) {
        Long parentId = loadAllFolderDTO.getFilePid();
        String currentFileIds = loadAllFolderDTO.getCurrentFileIds(); // 当前如果选中的是目录的话，是不能移动到当前目录的
        String[] fileOrFolderIds = currentFileIds.split(",");

        List<Folder> folders = folderMapper.selectList(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getParentId, parentId));

        List<GetFolderInfo> getFolderInfos = new ArrayList<>();
        for (Folder folder : folders) {
            getFolderInfos.add(new GetFolderInfo(folder.getName(), folder.getFolderId()));
        }

        return Result.success(getFolderInfos);
    }

    /**
     * 这个操作是可以批量操作的和单个操作的（对于是否可以重复文件名，看具体的需求，后续进行修改）
     * 这里其实目录和文件是一致的，需要对其分别加以判断相应的逻辑操作
     * @param fileIds
     * @param filePid
     * @return
     */
    @Override
    public Result changeFileFolder(String fileIds, Long filePid) {
        // 这里要注意一下非法目录的移动（对于资源的操作都要注意一下非法请求，增强代码的健壮性）
        if (filePid != Constants.ZERO) { // 这里是因为0是根目录 逻辑
            Folder folder = folderMapper.selectOne(new LambdaQueryWrapper<Folder>()
                    .eq(Folder::getFolderId, filePid));
            if (folder == null) {
                throw new BaseException(ResponseCodeEnum.CODE_600.getMsg());
            }
        }

        // 如果要检查是否重命名就在这里

        // 这里就是批量移动文件或者目录
        Long userId = BaseContext.getUserId();
        String[] fileId = fileIds.split(",");
        userFileMapper.updateBatchFolderId(userId, fileId, filePid); //这里是批量处理文件的移动的

        return Result.success();
    }
}
