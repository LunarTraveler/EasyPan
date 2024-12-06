package com.xcu.constants;

public class RedisConstant {

    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 5L;
    public static final String LOGIN_CAPTCHA_CODE_KEY = "login:captchaCode:";
    public static final Long LOGIN_CAPTCHA_CODE_TTL = 5L;
    public static final String REGISTER_CAPTCHA_CODE_KEY = "register:captchaCode:";
    public static final Long REGISTER_CAPTCHA_CODE_TTL = 10L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final String FILE_USEDSPACE_KEY = "file:usedspace:";
    public static final String TEMP_FILE_SIZE_KEY = "file:tempsize:";
    public static final Long TEMP_FILE_SIZE_TTL = 1L; // 一天

    public static final String DOWNLOAD_FILE_KEY = "download:file:";
    public static final Long DOWNLOAD_FILE_TTL = 10L; // 10分钟


}
