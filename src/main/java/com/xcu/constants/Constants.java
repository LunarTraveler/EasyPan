package com.xcu.constants;

public class Constants {

    // 开发过程中是在win平台上的(D:/DATA)，上线是在linux平台上的根目录(/DATA)
    public static final String FILE_ROOT_DIR = "D:/DATA";

    public static final String DEFAULT_AVATAR = "/images/easypan.jpg";

    public static final String AVATAR_DIR = "/images/";

    public static final String IMAGE_DIR = "images/";

    public static final String FILE_STORAGE = "/storage/files/";

    public static final String TEMP_DIR = "/temp/";

    public static final String AVATAR_SUFFIX = ".jpg";

    public static final String ID = "userId";

    public static final Long MB = 1024 * 1024L; // 1MB

    public static final Long totalSpace = 1024 * 1024 * 1024L; // 1GB

    public static final Long vipTotalSpace = totalSpace * 20; // 20GB

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String DOT = ".";

    public static final String SLASH = "/";

    public static final Integer LENGTH_150 = 150;

    public static final Long ZERO = 0L;

    public static final Long LIMIT_SPEED = 100 * 1024L; // 100KB/s

    public static final Long GLOBAL_LIMIT_SPEED = 10 * 1024 * 1024L; // 10MB/s既对应着服务器带宽为 80Mbps

}
