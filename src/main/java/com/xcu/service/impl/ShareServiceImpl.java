package com.xcu.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcu.context.BaseContext;
import com.xcu.entity.dto.LoadFileListDTO;
import com.xcu.entity.dto.SaveShareDTO;
import com.xcu.entity.dto.ShareFileDTO;
import com.xcu.entity.enums.ResponseCodeEnum;
import com.xcu.entity.enums.ShareValidTypeEnums;
import com.xcu.entity.pojo.FileFolder;
import com.xcu.entity.pojo.Share;
import com.xcu.entity.vo.GetFolderInfo;
import com.xcu.entity.vo.GetShareLoginInfoVO;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.entity.vo.LoadShareListVO;
import com.xcu.exception.BaseException;
import com.xcu.mapper.FileFolderMapper;
import com.xcu.mapper.FileMapper;
import com.xcu.mapper.ShareMapper;
import com.xcu.result.PageResult;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.service.ShareService;
import com.xcu.util.PageResultConversionUtil;
import com.xcu.util.RedisIdIncrement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {

    private final ShareMapper shareMapper;

    private final RedisIdIncrement redisIdIncrement;

    private final FileFolderMapper fileFolderMapper;

    @Override
    public Result loadShareList(Integer pageNo, Integer pageSize) {
        Long userId = BaseContext.getUserId();
        IPage<LoadShareListVO> page = new Page<>(pageNo, pageSize);
        page = shareMapper.loadShareList(page, userId);

        PageResult<LoadShareListVO> pageResult = new PageResult<>();
        pageResult.setPageNo((int)page.getCurrent());
        pageResult.setPageSize((int)page.getSize());
        pageResult.setTotalCount(page.getTotal());
        pageResult.setPageTotal(page.getPages());
        // 表示查出来的是文件列表 这里的封面图片路径要修改一下
        page.getRecords().forEach(e -> {
            e.setFileCover(e.getFileCover() != null ? e.getFileCover().substring(8) : null); // 只能返回两级目录
        });
        pageResult.setList(page.getRecords());

        return Result.success(pageResult);
    }

    @Override
    public Result shareFile(ShareFileDTO shareFileDTO) {
        Long fileId = shareFileDTO.getFileId();
        Integer validType = shareFileDTO.getValidType();
        String code = shareFileDTO.getCode();
        Integer days = ShareValidTypeEnums.getByType(validType).getDays();

        Long shareId = redisIdIncrement.nextId("share");
        Long userId = BaseContext.getUserId();
        LocalDateTime expireTime = null;

        if (days != ShareValidTypeEnums.FOREVER.getDays()) {
            expireTime = LocalDateTime.now().plusDays(days);
        }

        // 另外的两个字段都有默认值
        Share share = Share.builder()
                .shareId(shareId)
                .userId(userId)
                .fileId(fileId)
                .validType(validType)
                .expireTime(expireTime)
                .code(code)
                .build();
        shareMapper.insert(share);

        return Result.success(share);
    }

    @Override
    public Result cancelShare(String[] ids) {
        List<Long> collect = Arrays.stream(ids).mapToLong(Long::valueOf).boxed().collect(Collectors.toList());
        shareMapper.deleteBatchIds(collect);

        return Result.success();
    }

    @Override
    public Result getShareLoginInfo(Long shareId) {
        long userId = BaseContext.getUserId();

        GetShareLoginInfoVO loginInfo = shareMapper.getShareLoginInfo(shareId);
        if (loginInfo.getUserId() == userId) {
            loginInfo.setCurrentUser(true);
        } else {
            loginInfo.setCurrentUser(false);
        }
        loginInfo.setAvatar(loginInfo.getAvatar().substring(7));

        return Result.success(loginInfo);
    }

    /**
     * 这个不要求登录就可以看到信息（之后在mvc拦截器中要配置一下）
     * @param shareId
     * @return
     */
    @Override
    public Result getShareInfo(Long shareId) {
        GetShareLoginInfoVO shareInfo = shareMapper.getShareLoginInfo(shareId);
        return Result.success(shareInfo);
    }

    /**
     * 这里由登录的原因没办法展示出这个界面，需要是不是这个分享文件的用户才可以看到具体的文件
     * @param shareId
     * @param code
     * @return
     */
    @Override
    public Result checkShareCode(Long shareId, String code) {
        Share share = shareMapper.selectById(shareId);

        // 判断是否过期
        if (share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BaseException(ResponseCodeEnum.CODE_902.getMsg());
        }

        // 判断验证码是否正确
        if (!share.getCode().equals(code)) {
            throw new BaseException("分享码错误");
        }

        // 更新浏览次数(就是自增)
        shareMapper.updateShowIncrement(shareId);

        return Result.success();
    }

    /**
     * 这里尤其要注意权限的判断，不能越权限(不是太理解这里的验证)
     * 这里要区分一下文件和目录
     * @param loadFileListDTO
     * @return
     */
    @Override
    public Result loadFileList(LoadFileListDTO loadFileListDTO) {
        Long filePid = loadFileListDTO.getFilePid();
        Long shareId = loadFileListDTO.getShareId();
        Share share = shareMapper.selectById(shareId);
        Long fileId = share.getFileId();

        IPage<LoadDataListVO> page = new Page<>(loadFileListDTO.getPageNo(), loadFileListDTO.getPageSize());

        if (filePid == null || filePid == 0) {
            // 这里代表的就是文件
            page = fileFolderMapper.selectFileInfoPage(page, null, null, fileId, null, null, null);
        } else {
            // 这里代表的就是目录
            page = fileFolderMapper.selectFileInfoPage(page, null, filePid, null, null, null, null);
        }

        PageResult<LoadDataListVO> pageResult = PageResultConversionUtil.conversion(page, LoadDataListVO.class);
        page.getRecords().forEach(e -> {
            e.setFileCover(e.getFileCover() != null ? e.getFileCover().substring(8) : null);
        });
        pageResult.setList(page.getRecords());

        return Result.success(pageResult);
    }

    @Override
    public Result getFolderInfo(Long shareId, String path) {
        Share share = shareMapper.selectById(shareId);
        String[] fileIds = path.split("/");

        List<GetFolderInfo> folderInfos = fileFolderMapper.getFolderInfo(fileIds, share.getUserId());
        return Result.success(folderInfos);
    }

    /**
     * 这个方法要进行参数可行性判断
     * @param saveShareDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result saveShare(SaveShareDTO saveShareDTO) {
        Long shareId = saveShareDTO.getShareId();
        String shareFileIds = saveShareDTO.getShareFileIds();
        Long myFolderId = saveShareDTO.getMyFolderId();
        Long userId = BaseContext.getUserId();

        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getUserId, userId)
                .eq(FileFolder::getFileFolderId, myFolderId));
        if (fileFolder == null) {
            throw new BaseException(ResponseCodeEnum.CODE_600.getMsg());
        }

        List<Long> ids = Arrays.stream(shareFileIds.split(","))
                .map(Long::parseLong) // 或者 Long::valueOf
                .collect(Collectors.toList());

        List<FileFolder> fileFolders = fileFolderMapper.selectBatchIds(ids);
        for (FileFolder ff : fileFolders) {
            ff.setId(null);
            ff.setParentId(myFolderId);
            ff.setUserId(userId);
        }

        fileFolderMapper.insertBatchs(fileFolders);

        return Result.success();
    }

}
