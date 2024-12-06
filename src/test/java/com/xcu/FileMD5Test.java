package com.xcu;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.FileSystemUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcu.constants.Constants;
import com.xcu.entity.enums.FileStatusEnums;
import com.xcu.entity.enums.FileTypeEnums;
import com.xcu.entity.pojo.FileInfo;
import com.xcu.mapper.FileMapper;
import com.xcu.util.ProcessUtils;
import com.xcu.util.ScaleFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
public class FileMD5Test {

    @Autowired
    private FileMapper fileMapper;

    @Test
    public void fileMD5Test() {
        String fileName = "D:/DATA/storage/files/3a/3a00fff2a3992191a70efe2891dbf05e.mp4";
        long fileId = 13465477130L;
        generateCover(fileName, fileId);
    }

    /**
     *
     * 对于视频和图片文件生成缩略图 并且对视频文件切割
     * @param fileName 这里是文件的全路径名称
     * @param fileId
     */
    public void generateCover(String fileName, long fileId) {
        System.out.println(FileNameUtil.getSuffix(fileName));
        FileTypeEnums fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(Constants.DOT + FileNameUtil.getSuffix(fileName));
        if (FileTypeEnums.VIDEO == fileTypeEnum) {
            // 对视频文件切割
            cutFileForVideo(fileId, fileName);

            // 抽取一帧作为视频的封面
            String coverPath = Constants.FILE_ROOT_DIR + Constants.AVATAR_DIR + UUID.fastUUID() + Constants.AVATAR_SUFFIX;
            ScaleFilter.createCover4Video(new File(fileName), Constants.LENGTH_150, new File(coverPath));
        } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
            // 直接对于这个图片生成一个缩略图
            String coverPath = Constants.FILE_ROOT_DIR + Constants.AVATAR_DIR + UUID.fastUUID() + Constants.AVATAR_SUFFIX;
            Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(fileName), Constants.LENGTH_150, new File(coverPath), false);
            if (!created) {
                FileUtil.copyFile(fileName, coverPath);
            }
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


}
