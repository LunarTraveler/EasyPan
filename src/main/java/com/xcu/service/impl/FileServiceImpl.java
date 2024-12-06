package com.xcu.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.UUID;
import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcu.constants.Constants;
import com.xcu.constants.RedisConstant;
import com.xcu.context.BaseContext;
import com.xcu.entity.dto.LoadAllFolderDTO;
import com.xcu.entity.dto.LoadDataListDTO;
import com.xcu.entity.dto.NewFolderDTO;
import com.xcu.entity.dto.UploadFileDTO;
import com.xcu.entity.enums.*;
import com.xcu.entity.pojo.FileInfo;
import com.xcu.entity.pojo.Folder;
import com.xcu.entity.pojo.UserWithFile;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.entity.vo.UploadFileVO;
import com.xcu.exception.BaseException;
import com.xcu.mapper.*;
import com.xcu.result.PageResult;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.util.ProcessUtils;
import com.xcu.util.RedisIdIncrement;
import com.xcu.util.ScaleFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileService {

    private final FileMapper fileMapper;

    private final UserFileMapper userFileMapper;

    private final FolderMapper folderMapper;

    private final FileFolderMapper fileFolderMapper;

    private final UserServiceImpl userServiceImpl;

    private final RedisIdIncrement redisIdIncrement;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<PageResult<LoadDataListVO>> loadDataList(LoadDataListDTO loadDataListDTO) {
        String category = loadDataListDTO.getCategory(); // all
        if ("all".equals(category)) {
            category = null; // 相当与这个条件就不会加入到其中
        }
        Long filePid = loadDataListDTO.getFilePid(); // 默认是0根目录
        String fileName = loadDataListDTO.getFileName(); // 可能是null
        Integer pageNo = loadDataListDTO.getPageNo();
        Integer pageSize = loadDataListDTO.getPageSize();

        // 多表连表的分页查询
        IPage<LoadDataListVO> page = new Page<>(
                pageNo == null ? 1 : pageNo,
                pageSize == null ? 15 : pageSize);
        page = fileMapper.selectFileInfoPage(page, category, filePid, fileName);

        PageResult<LoadDataListVO> pageResult = new PageResult<>();
        pageResult.setPageNo(pageNo);
        pageResult.setPageSize(pageSize);
        pageResult.setTotalCount(page.getTotal());
        pageResult.setPageTotal(page.getPages());
        // 表示查出来的是文件列表 这里的封面图片路径要修改一下
        page.getRecords().forEach(e -> {
            e.setFileCover(e.getFileCover() != null ? e.getFileCover().substring(8) : null); // 只能返回两级目录
        });
        pageResult.setList(page.getRecords());

        return Result.success(pageResult);
    }

    @Transactional
    @Override
    public Result uploadFile(UploadFileDTO uploadFileDTO) {
        // 获取前端传来的参数，后面使用会比较方便
        String fileIdStr = uploadFileDTO.getFileId(); // 这个可能会为空
        Long fileId = redisIdIncrement.nextId("file");
        MultipartFile file = uploadFileDTO.getFile();
        String fileName = uploadFileDTO.getFileName();
        Long filePid = Long.valueOf(uploadFileDTO.getFilePid());
        String fileMd5 = uploadFileDTO.getFileMd5();
        Integer chunkIndex = uploadFileDTO.getChunkIndex();
        Integer chunks = uploadFileDTO.getChunks();

        // 文件不是第一个分块的话
        if (!StringUtils.isEmpty(fileIdStr)) {
            fileId = Long.parseLong(fileIdStr);
        }

        // 当前用户的id和当前的使用空间大小 临时文件的大小
        Long userId = BaseContext.getUserId();
        Integer usedSpace = Integer.valueOf(stringRedisTemplate.opsForValue().get(RedisConstant.FILE_USEDSPACE_KEY + userId));
        stringRedisTemplate.opsForValue().increment(RedisConstant.TEMP_FILE_SIZE_KEY + userId, file.getSize());
        Long tempFileSize = Long.valueOf(stringRedisTemplate.opsForValue().get(RedisConstant.TEMP_FILE_SIZE_KEY + userId));

        // 判断这个文件整个文件表中是否已经存在（秒传）
        if (chunkIndex == 0) {
            FileInfo secFile = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileMd5, fileMd5)
                    .eq(FileInfo::getStatus, FileStatusEnums.USING.getStatus()));
            if (secFile != null) {
                if (secFile.getFileSize() + usedSpace > Constants.totalSpace) {
                    throw new BaseException(ResponseCodeEnum.CODE_904.getMsg());
                }
                // 实现了一份文件具有多个逻辑引用（注意删除的时候是要逻辑删除）
                long id = redisIdIncrement.nextId("userFile");
                UserWithFile userWithFile = UserWithFile.builder()
                        .id(id)
                        .userId(userId)
                        .fileId(fileId)
                        .fileName(fileName)
                        .filePid(filePid)
                        .status(FileStatusEnums.USING.getStatus())
                        .build();
                userFileMapper.insert(userWithFile);

                // 增加文件的引用次数(注意要加悲观锁的，防止多个人上传同一个文件导致的数据不一致问题)
                fileMapper.updateRefCount(fileMd5);

                // 更新用户的空间
                usedSpace = (int) (usedSpace + secFile.getFileSize());
                userServiceImpl.updateUsedSpace(userId, usedSpace);

                return Result.success(new UploadFileVO(fileId, UploadStatusEnums.UPLOAD_SECONDS.getCode()));
            }
        }

        // 把上传的文件先保存在临时目录中
        String tempFileFolder = Constants.FILE_ROOT_DIR + Constants.TEMP_DIR + userId + "_" + fileId;
        Path tempFilePath = Paths.get(Constants.FILE_ROOT_DIR, Constants.TEMP_DIR, userId + "_" + fileId, String.valueOf(chunkIndex));
        if (!tempFilePath.toFile().exists()) {
            tempFilePath.toFile().mkdir();
        }
        File tempFile = new File(tempFileFolder);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }

        if (tempFileSize + usedSpace > Constants.totalSpace) {
            throw new BaseException(ResponseCodeEnum.CODE_904.getMsg());
        }

        File newFile = new File(tempFileFolder + "/" + chunkIndex);
        try {
            file.transferTo(newFile);
        } catch (IOException e) {
            log.error("文件上传失败 {}", e.getMessage());
            throw new BaseException("文件上传失败");
        }

        // 还有其他的分片没有上传
        if (chunkIndex < chunks - 1) {
            return Result.success(new UploadFileVO(fileId, UploadStatusEnums.UPLOADING.getCode()));
        }

        // 现在所得文件分片都在临时目录中，合并文件
        String targetFolder = Constants.FILE_ROOT_DIR + Constants.FILE_STORAGE + fileMd5.substring(0, 2) + "/";
        String targetFileName = fileMd5 + "." + FileUtil.getSuffix(fileName);
        try {
            // 注意aop代理实现的异步合并文件（默认使用的是单线程，也可以配置一个线程池）
            FileService fileService = (FileService) AopContext.currentProxy();
            fileService.unionFile(tempFileFolder, targetFolder, targetFileName, chunks, fileId);

            FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(Constants.DOT + FileNameUtil.getSuffix(fileName));
            // 数据库插入文件信息  文件与用户的信息  用户的空间信息
            FileInfo fileInfo = FileInfo.builder()
                    .fileId(fileId)
                    .fileMd5(fileMd5)
                    .fileSize(tempFileSize)
                    .filePath(targetFolder + targetFileName)
                    .status(FileStatusEnums.TRANSFER.getStatus())
                    .refCount(1)
                    .fileCategory(fileTypeEnum.getCategory().getCategory())
                    .fileType(fileTypeEnum.getType())
                    .build();
            fileMapper.insert(fileInfo);

            long id = redisIdIncrement.nextId("userFile");
            UserWithFile userWithFile = UserWithFile.builder()
                    .id(id)
                    .userId(userId)
                    .fileId(fileId)
                    .fileName(fileName)
                    .filePid(filePid)
                    .status(FileStatusEnums.USING.getStatus())
                    .build();
            userFileMapper.insert(userWithFile);

            userServiceImpl.updateUsedSpace(userId, (int)(usedSpace + tempFileSize));
        } catch (Exception e) {
            log.error("文件合并失败 {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            // 不管文件上传是否成功都要删除临时目录，实现简单
            // FileUtil.del(Paths.get(tempFileFolder)); 这里要注意异步任务使用的资源要在他使用完之后在关闭
            stringRedisTemplate.delete(RedisConstant.TEMP_FILE_SIZE_KEY + userId);
        }

        return Result.success(new UploadFileVO(fileId, UploadStatusEnums.UPLOAD_FINISH.getCode()));
    }

    @Async
    @Override
    public void unionFile(String tempFileFolder, String targetFolder, String targetFileName, int chunks, long fileId) throws Exception {
        String targetPath = targetFolder + targetFileName;
        Path path = Paths.get(targetPath);

        // 按照MD5值的前两个字符作为一块文件夹  MD5作为文件名
        File folder = new File(targetFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(targetPath);
        if (!file.exists()) {
            file.createNewFile();
        }

        // 打开目标文件，以追加模式写入
        try (FileChannel targetChannel = FileChannel.open(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {

            ByteBuffer buffer = ByteBuffer.allocate(256 * 1024); // 缓冲区大小：256KB
            for (int i = 0; i < chunks; i++) {
                Path sourcePath = Paths.get(tempFileFolder + "/" + i); // 使用 Paths.get(8) 替代 Path.of(11)
                // 打开源文件进行读取
                try (FileChannel sourceChannel = FileChannel.open(sourcePath, StandardOpenOption.READ)) {
                    while (sourceChannel.read(buffer) > 0) {
                        buffer.flip(); // 切换到读模式
                        targetChannel.write(buffer); // 写入到目标文件
                        buffer.clear(); // 清空缓冲区
                    }
                }
            }
        } finally {
            FileUtil.del(Paths.get(tempFileFolder));
        }

        // 如果是视频文件的话要把原视频生成封面和视频切割
        generateCover(targetPath, fileId);
    }

    /**
     *
     * 对于视频和图片文件生成缩略图 并且对视频文件切割
     * @param fullPathName 这里是文件的全路径名称
     * @param fileId
     */
    public void generateCover(String fullPathName, long fileId) {
        FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(Constants.DOT + FileNameUtil.getSuffix(fullPathName));
        String coverPath = null;
        if (FileTypeEnums.VIDEO == fileTypeEnum) {
            // 对视频文件切割
            cutFileForVideo(fileId, fullPathName);

            // 抽取一帧作为视频的封面
            coverPath = Constants.FILE_ROOT_DIR + Constants.AVATAR_DIR + UUID.fastUUID() + Constants.AVATAR_SUFFIX;
            ScaleFilter.createCover4Video(new File(fullPathName), Constants.LENGTH_150, new File(coverPath));
        } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
            // 直接对于这个图片生成一个缩略图
            coverPath = Constants.FILE_ROOT_DIR + Constants.AVATAR_DIR + UUID.fastUUID() + Constants.AVATAR_SUFFIX;
            Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(fullPathName), Constants.LENGTH_150, new File(coverPath), false);
            if (!created) {
                FileUtil.copyFile(fullPathName, coverPath);
            }
        }

        if (coverPath != null) {
            fileMapper.updateById(FileInfo.builder()
                    .fileId(fileId)
                    .fileCover(coverPath)
                    .status(FileStatusEnums.USING.getStatus())
                    .build());
        }
    }

    public void cutFileForVideo(long fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }

        //vbsf改成-bsf
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -bsf:v h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);

        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);

        //删除index.ts
        FileUtil.del(tsPath);
    }

    @Override
    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        Path path = Paths.get(Constants.FILE_ROOT_DIR, imageFolder, imageName);

        // 检查文件是否存在
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 设置响应头
        response.setContentType("image/" + FileNameUtil.getSuffix(imageName));
        response.setHeader("Cache-Control", "max-age=60");

        // 写入文件到输出流
        conversionOutput(path, response);
    }

    /**
     * 这里就是正常的返回一个文件就行
     * @param response
     * @param fileId
     */
    @Override
    public void getFile(HttpServletResponse response, Long fileId) {
        FileInfo fileInfo = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId));
        Path path = Paths.get(fileInfo.getFilePath());

        // 写入文件到输出流
        conversionOutput(path, response);
    }

    /**
     * 这里是视频文件独有的一个接口，用于返回同一个同名目录下的ts文件和m3u8文件
     * @param response
     * @param fileId
     */
    @Override
    public void getVideoInfo(HttpServletResponse response, String fileId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            // 代表是具体的ts视频切片文件
            String realId = fileId.substring(0, fileId.lastIndexOf("_"));
            FileInfo fileInfo = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, realId));
            filePath = fileInfo.getFilePath(); // 这里是源视频文件的存储位置
            filePath = filePath.substring(0, filePath.lastIndexOf(".")) + "/" + fileId;
        } else {
            // 代表是第一次访问的，那么就返回m3u8索引文件
            FileInfo fileInfo = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId));
            filePath = fileInfo.getFilePath();
            filePath = filePath.substring(0, filePath.lastIndexOf(".")) + "/" + Constants.M3U8_NAME;
        }

        // 写入文件到输出流（这里可能有大文件但是不是视频的话，就涉及到分片下载）
        Path path = Paths.get(filePath);
        conversionOutput(path, response);
    }

    /**
     * 把一个文件流输出到响应流中
     * @param path
     * @param response
     */
    private void conversionOutput(Path path, HttpServletResponse response) {
        // 写入文件到输出流
        try (InputStream inputStream = Files.newInputStream(path)) {
            IoUtil.copy(inputStream, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            // 错误处理
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("文件到输出流错误 {}", e.getMessage());
        }
    }

    /**
     * 这里用于区分文件和目录（这里就是分表之后的弊端了没办法）
     * @param fileId
     * @return
     */
    private Boolean isFile(Long fileId) {
        return fileId > 1000000L;
    }

    /**
     * 这里可以同时修改文件和文件夹，两个共用一个接口
     */
    @Override
    public Result rename(Long fileId, String newName) {
        // 这里没办法了，由于共用一个接口，只能这样做一个区分了
        if (isFile(fileId)) {
            return reFileName(fileId, newName);
        } else {
            return reFolderName(fileId, newName);
        }
    }

    private Result reFileName(Long fileId, String fileName) {
        Long userId = BaseContext.getUserId();
        UserWithFile userWithFile = userFileMapper.selectOne(new LambdaQueryWrapper<UserWithFile>()
                .eq(UserWithFile::getFileId, fileId)
                .eq(UserWithFile::getUserId, userId));
        if (userWithFile == null) {
            throw new BaseException("这个文件不存在");
        }

        fileName = fileName + Constants.DOT + FileNameUtil.getSuffix(userWithFile.getFileName());

        int updateRows = userFileMapper.update(UserWithFile.builder().fileName(fileName).build(),
                new LambdaQueryWrapper<UserWithFile>()
                        .eq(UserWithFile::getUserId, userId)
                        .eq(UserWithFile::getFileId, fileId));
        if (updateRows == 0) {
            throw new BaseException("文件重命名失败");
        }
        return Result.success();
    }

    private Result reFolderName(Long folderId, String folderName) {
        Folder folder = folderMapper.selectOne(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getFolderId, folderId));
        if (folder == null) {
            throw new BaseException("这个文件夹不存在");
        }

        Folder folderQuery = new Folder();
        folderQuery.setFolderId(folderId);
        folderQuery.setName(folderName);
        int updateRows = folderMapper.updateById(folderQuery);
        if (updateRows == 0) {
            throw new BaseException("文件目录重命名失败");
        }
        return Result.success();
    }

    /**
     * 这里也是可以批量操作的，注意区分文件和目录
     * 注意文件是逻辑删除的，但是目录不是逻辑删除，用户对应的云盘空间不会改变（方便还原文件）
     * 这里是只能来选中一层的文件或是目录
     * @param fileIds
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result delFile(String fileIds) {
        // 把两个表的东西筛选出来吧，之后是可以实现批量操作的
        String[] fileIdArray = fileIds.split(",");

        Map<Boolean, List<Long>> collect = Arrays.stream(fileIdArray)
                .map(Long::valueOf)
                .collect(Collectors.partitioningBy(id -> isFile(id)));

        // 先要对目录进行操作可以获取到里面的文件id
        List<Long> ids = deleteFolder(collect.get(false));

        // 对于全部的文件进行逻辑删除
        List<Long> fileIdList = collect.get(true);
        fileIdList.addAll(ids);

        // aop实现事务的嵌套关系(事务的传播行为)
        FileService fileService = (FileService) AopContext.currentProxy();
        fileService.deleteLogicFile(fileIdList);

        return Result.success();
    }

    /**
     * 对于文件来说直接赋值为逻辑删除就行
     * @param fileIds
     * @return
     */
    public void deleteLogicFile(List<Long> fileIds) {
        Long userId = BaseContext.getUserId();
        Integer status = FileDelFlagEnums.RECYCLE.getFlag();
        LocalDateTime recoveryTime = LocalDateTime.now();

        userFileMapper.updateBatchStatus(fileIds, userId, status, recoveryTime);
    }

    /**
     * 对于目录来说要判断目录里是否有文件（递归查询） 没有的话直接删除就行了
     * Propagation.REQUIRED -> 加入事务(同一个事务 默认的)
     * Propagation.REQUIRES_NEW -> 独立新建事务(不同的事务)
     * @param folderIds
     * @return 返回的是文件的集合
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<Long> deleteFolder(List<Long> folderIds) {
        List<Long> fileIdList = new ArrayList<>();
        List<Long> folderIdList = new ArrayList<>();

        // 对于当前这一层的全部目录都要做一个递归查询
        for (Long folderId : folderIds) {
            List<Long> ids = folderMapper.recursiveQuery(folderId);
            for (Long id : ids) {
                if (isFile(id)) {
                    fileIdList.add(id);
                } else {
                    folderIdList.add(id);
                }
            }
        }

        // 目录直接删除就行了，不用设置逻辑删除
        folderMapper.deleteBatchIds(folderIdList);

        return fileIdList;
    }

    @Override
    public Result createDownloadUrl(Long fileId) {
        // 防止下载越权限
        Long userId = BaseContext.getUserId();
        UserWithFile userWithFile = userFileMapper.selectOne(new LambdaQueryWrapper<UserWithFile>()
                .eq(UserWithFile::getUserId, userId)
                .eq(UserWithFile::getFileId, fileId));
        if (userWithFile == null) {
            throw new BaseException("文件不存在，请检查下载文件");
        }

        String code = UUID.randomUUID(true).toString().replaceAll("-", "");
        stringRedisTemplate.opsForValue().set(RedisConstant.DOWNLOAD_FILE_KEY + code, fileId.toString(),
                RedisConstant.DOWNLOAD_FILE_TTL, TimeUnit.MINUTES);

        return Result.success(code);
    }

    /**
     * 对于大文件进行分块下载(后续把分片的思路完成)
     * @param code
     * @param response
     */
    @Override
    public void download(String code, HttpServletResponse response) throws IOException {
        String fileId = stringRedisTemplate.opsForValue().get(RedisConstant.DOWNLOAD_FILE_KEY + code);
        if (StringUtils.isEmpty(fileId)) {
            throw new BaseException("提取码已过期，请重试");
        }

        FileInfo fileInfo = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getFileId, fileId));
        Path path = Paths.get(fileInfo.getFilePath());

        // 设置响应头(类型也是要设置的全面一点)
        String fileType = Files.probeContentType(path);
        if (fileType == null) {
            fileType = "application/octet-stream";
        }
        response.setContentType(fileType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + path.getFileName().toString() + "\"");
        response.setContentLengthLong(fileInfo.getFileSize());
        conversionOutput(path, response);
    }

    @Override
    public Result newFolder(NewFolderDTO newFolderDTO) {
        return null;
    }

    @Override
    public Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO) {
        return null;
    }

    @Override
    public Result changeFileFolder(String fileIds, Long filePid) {
        return null;
    }
}
