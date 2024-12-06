package com.xcu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@SpringBootTest
public class FileChannelTest {

    @Test
    public void test() throws Exception {
        this.unionFile("D:/DATA/temp/10349682689_12428488708",
                "D:/DATA/storage/files/ab/",
                "abvassacpcasjp.mp4",
                8);
    }

    public void unionFile(String tempFileFolder, String targetFolder, String targetFileName, int chunks) throws Exception {
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
        }
    }

}
