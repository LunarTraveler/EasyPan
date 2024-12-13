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
import com.xcu.entity.dto.*;
import com.xcu.entity.enums.*;
import com.xcu.entity.pojo.FileFolder;
import com.xcu.entity.pojo.FileInfo;
import com.xcu.entity.vo.GetFolderInfo;
import com.xcu.entity.vo.LoadDataListVO;
import com.xcu.entity.vo.UploadFileVO;
import com.xcu.exception.BaseException;
import com.xcu.mapper.*;
import com.xcu.result.PageResult;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.util.PageResultConversionUtil;
import com.xcu.util.ProcessUtils;
import com.xcu.util.RedisIdIncrement;
import com.xcu.util.ScaleFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileService {

    private final FileMapper fileMapper;

    private final FileFolderMapper fileFolderMapper;

    private final UserServiceImpl userServiceImpl;

    private final RedisIdIncrement redisIdIncrement;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result<PageResult<LoadDataListVO>> loadDataList(LoadDataListDTO loadDataListDTO) {
        String categoryParam = loadDataListDTO.getCategory(); // all
        Integer category = null; // 如果传来的是all 那么的话就直接是null了
        if (!StringUtils.isEmpty(categoryParam) && !"all".equals(categoryParam)) {
            category = FileCategoryEnums.getByCode(categoryParam).getCategory();
        }
        Long filePid = loadDataListDTO.getFilePid(); // 默认是0根目录
        String fileName = loadDataListDTO.getFileName(); // 可能是null
        Integer pageNo = loadDataListDTO.getPageNo();
        Integer pageSize = loadDataListDTO.getPageSize();
        Long userId = BaseContext.getUserId();

        // 多表连表的分页查询
        IPage<LoadDataListVO> page = new Page<>(
                pageNo == null ? 1 : pageNo,
                pageSize == null ? 15 : pageSize);
        page = fileFolderMapper.selectFileInfoPage(page, category, filePid,null, fileName, userId, null);

        PageResult<LoadDataListVO> pageResult = new PageResult<>();
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

    @Transactional
    @Override
    public Result uploadFile(UploadFileDTO uploadFileDTO) {
        // 获取前端传来的参数，后面使用会比较方便
        String fileIdStr = uploadFileDTO.getFileId(); // 这个可能会为空
        Long fileId = redisIdIncrement.nextId(RedisConstant.FILE_OR_FOLDER_KEY);
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

        // 判断这个文件整个文件表中是否已经存在（秒传的逻辑）
        if (chunkIndex == 0) {
            FileInfo secFile = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileMd5, fileMd5)
                    .eq(FileInfo::getStatus, FileStatusEnums.USING.getStatus()));
            if (secFile != null) {
                if (secFile.getFileSize() + usedSpace > Constants.totalSpace) {
                    throw new BaseException(ResponseCodeEnum.CODE_904.getMsg());
                }
                // 实现了一份文件具有多个逻辑引用（注意删除的时候是要逻辑删除）
                FileFolder fileFolder = FileFolder.builder()
                        .userId(userId)
                        .fileFolderId(fileId)
                        .name(fileName)
                        .parentId(filePid)
                        .isDirectory(FileFolderTypeEnums.FILE.getType())
                        .status(FileStatusEnums.USING.getStatus())
                        .build();
                fileFolderMapper.insert(fileFolder);

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

            // 实现了一份文件具有多个逻辑引用（注意删除的时候是要逻辑删除）
            FileFolder fileFolder = FileFolder.builder()
                    .userId(userId)
                    .fileFolderId(fileId)
                    .name(fileName)
                    .parentId(filePid)
                    .isDirectory(FileFolderTypeEnums.FILE.getType())
                    .status(FileStatusEnums.USING.getStatus())
                    .build();
            fileFolderMapper.insert(fileFolder);

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

        // fileMapper.update(FileInfo.builder().build())
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
     * 这里可以同时修改文件和文件夹，两个共用一个接口
     */
    @Override
    public Result rename(Long fileId, String newName) {
        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getFileFolderId, fileId));

        if (fileFolder.getIsDirectory() == FileFolderTypeEnums.FILE.getType()) {
            return reFileName(fileId, newName);
        } else {
            return reFolderName(fileId, newName);
        }
    }

    private Result reFileName(Long fileId, String fileName) {
        Long userId = BaseContext.getUserId();
        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getFileFolderId, fileId)
                .eq(FileFolder::getUserId, userId));
        if (fileFolder == null) {
            throw new BaseException("这个文件不存在");
        }

        fileName = fileName + Constants.DOT + FileNameUtil.getSuffix(fileFolder.getName());

        fileFolder.setName(fileName);
        int updateRows = fileFolderMapper.updateById(fileFolder);
        if (updateRows == 0) {
            throw new BaseException("文件重命名失败");
        }
        return Result.success();
    }

    private Result reFolderName(Long folderId, String folderName) {
        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getFileFolderId, folderId));
        if (fileFolder == null) {
            throw new BaseException("这个文件夹不存在");
        }

        fileFolder.setName(folderName);
        int updateRows = fileFolderMapper.updateById(fileFolder);
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
        String[] idArray = fileIds.split(",");

        // 递归查询加更新
        fileFolderMapper.recursiveFileInRecovery(idArray);

        return Result.success();
    }

    @Override
    public Result createDownloadUrl(Long fileId) {
        // 防止下载越权限
        Long userId = BaseContext.getUserId();
        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getUserId, userId)
                .eq(FileFolder::getFileFolderId, fileId));
        if (fileFolder == null) {
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
        String name = newFolderDTO.getFileName();
        checkRepeatName(name, FileFolderTypeEnums.FOLDER.getType());

        Long folderId = redisIdIncrement.nextId(RedisConstant.FILE_OR_FOLDER_KEY);
        FileFolder folder = FileFolder.builder()
                .fileFolderId(folderId)
                .name(name)
                .parentId(newFolderDTO.getFilePid())
                .userId(BaseContext.getUserId())
                .isDirectory(FileFolderTypeEnums.FOLDER.getType())
                .status(FileStatusEnums.USING.getStatus())
                .build();

        fileFolderMapper.insert(folder);

        return Result.success();
    }

    /**
     * 检查文件或是目录名称是否重复
     * @param name
     */
    private void checkRepeatName(String name, int isDirectory) {
        Long userId = BaseContext.getUserId();
        FileFolder fileFolder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getIsDirectory, isDirectory)
                .eq(FileFolder::getUserId, userId)
                .eq(FileFolder::getName, name));
        if (fileFolder != null) {
            if (isDirectory == FileFolderTypeEnums.FOLDER.getType()) {
                throw new BaseException("此目录名已经存在，请在起一个新的名称");
            } else {
                throw new BaseException("此文件名已经存在，请在起一个新的名称");
            }
        }

    }

    // TODO 可能有问题 fileOrFolderIds这个没有用到
    @Override
    public Result loadAllFolder(LoadAllFolderDTO loadAllFolderDTO) {
        Long parentId = loadAllFolderDTO.getFilePid();
        String currentFileIds = loadAllFolderDTO.getCurrentFileIds(); // 当前如果选中的是目录的话，是不能移动到当前目录的
        String[] fileOrFolderIds = currentFileIds.split(",");

        List<FileFolder> folders = fileFolderMapper.selectList(new LambdaQueryWrapper<FileFolder>()
                .eq(FileFolder::getIsDirectory, FileFolderTypeEnums.FOLDER.getType())
                .eq(FileFolder::getParentId, parentId));

        List<GetFolderInfo> getFolderInfos = new ArrayList<>();
        for (FileFolder folder : folders) {
            getFolderInfos.add(new GetFolderInfo(folder.getName(), folder.getFileFolderId()));
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
            FileFolder folder = fileFolderMapper.selectOne(new LambdaQueryWrapper<FileFolder>()
                    .eq(FileFolder::getIsDirectory, FileFolderTypeEnums.FOLDER.getType())
                    .eq(FileFolder::getFileFolderId, filePid));
            if (folder == null) {
                throw new BaseException(ResponseCodeEnum.CODE_600.getMsg());
            }
        }

        // 如果要检查是否重命名就在这里

        // 这里就是批量移动文件或者目录
        Long userId = BaseContext.getUserId();
        String[] fileId = fileIds.split(",");
        fileFolderMapper.updateBatchFolderId(userId, fileId, filePid); //这里是批量处理文件的移动的

        return Result.success();
    }

    @Override
    public Result getFolderInfo(String path) {
        Long userId = BaseContext.getUserId();
        String[] folderIds = path.split("/");
        List<GetFolderInfo> folderInfos = fileFolderMapper.getFolderInfo(folderIds, userId);

        return Result.success(folderInfos);
    }

    /**
     * 这里是很复杂的，相当于一颗树我们只要发现第一个是逻辑删除的节点，其他的节点都可以不展示
     * 在二维表的形式中，可以使用这条记录的pid没有被删除就完美解决了这个树形问题了
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result loadRecycleList(Integer pageNo, Integer pageSize) {
        Long userId = BaseContext.getUserId();
        // 直接查出来主键的id，免得二次查找了
        List<Long> ids = fileFolderMapper.getFirstMatchingNodeEncountered(userId);
        IPage<LoadDataListVO> page = new Page(pageNo, pageSize);
        page = fileFolderMapper.loadRecycleList(page, ids);

        PageResult<LoadDataListVO> pageResult = new PageResult<>();
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

    /**
     * 回收文件或是目录（这里要向下进行递归查询所有状态的修改）
     * @param fileFolders
     * @return
     */
    @Override
    public Result recoverFile(String[] fileFolders) {
        // 递归的修改状态为正常使用的状态
        fileFolderMapper.recursiveFileOutRecovery(fileFolders);
        return Result.success();
    }

    /**
     * 彻底的删除文件（这里要向下进行递归查询所有状态的修改） 如果是文件也要删除的话开启一个定时任务来做的
     * @param fileFolders
     * @return
     */
    @Override
    public Result completeDelFile(String[] fileFolders) {
        fileFolderMapper.recursiveCompleteDelFile(fileFolders);
        return Result.success();
    }

    /**
     * 查询出所有的 *文件*
     * @param loadFileListDTO
     * @return
     */
    @Override
    public Result loadFileList(LoadFileListDTO loadFileListDTO) {
        IPage<LoadDataListVO> page = new Page<>(loadFileListDTO.getPageNo(), loadFileListDTO.getPageSize());
        page = fileFolderMapper.selectFileInfoPage(page, null, null, null, null, null, 0);

        PageResult<LoadDataListVO> pageResult = PageResultConversionUtil.conversion(page, LoadDataListVO.class);
        // 表示查出来的是文件列表 这里的封面图片路径要修改一下
        page.getRecords().forEach(e -> {
            e.setFileCover(e.getFileCover() != null ? e.getFileCover().substring(8) : null); // 只能返回两级目录(这里在nginx中是有映射的)
        });
        pageResult.setList(page.getRecords());

        return Result.success(pageResult);
    }

    /**
     * 这里设计的不好，管理员应该能够直接下载而不需要code验证码
     * @param response
     * @param fileId
     * @throws IOException
     */
    @Override
    public void adminDownload(HttpServletResponse response, Long fileId) throws IOException {
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
}
