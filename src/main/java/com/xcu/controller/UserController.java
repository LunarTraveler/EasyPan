package com.xcu.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.xcu.constants.Constants;
import com.xcu.constants.RedisConstant;
import com.xcu.entity.dto.LoginDTO;
import com.xcu.entity.dto.RegisterDTO;
import com.xcu.entity.dto.ResetPwdDTO;
import com.xcu.entity.dto.SendEmailCodeDTO;
import com.xcu.result.Result;
import com.xcu.service.FileService;
import com.xcu.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final FileService fileService;

    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        // 定义图形验证码的长和宽
        LineCaptcha vCode = CaptchaUtil.createLineCaptcha(130, 38, 5, 10);

        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpge");

        // 这里的图形验证码也是分了两种 一个是获取邮箱的验证码和登录验证码 一个是注册验证码
        // 这里是由问题，如果按照这种思路的那话，那么就必须要使用的session会话技术
        String code = vCode.getCode();
        log.info("图形验证码是 {}", code);
        if (type == null || type == 0) {
            session.setAttribute(RedisConstant.REGISTER_CAPTCHA_CODE_KEY, code);
        } else {
            session.setAttribute(RedisConstant.LOGIN_CAPTCHA_CODE_KEY, code);
        }

        // 图形验证码写出，可以写出到文件，也可以写出到流
        vCode.write(response.getOutputStream());
        response.getOutputStream().close();
    }

    @PostMapping("/sendEmailCode")
    public Result sendEmailCode(@Validated SendEmailCodeDTO sendEmailCodeDTO, HttpSession session) {
        String code = (String) session.getAttribute(RedisConstant.LOGIN_CAPTCHA_CODE_KEY);
        return userService.sendEmailCode(sendEmailCodeDTO, code);
    }

    @PostMapping("/register")
    public Result register(@Validated RegisterDTO registerDTO, HttpSession session) {
        String code = (String) session.getAttribute(RedisConstant.REGISTER_CAPTCHA_CODE_KEY);
        log.info("code :{}", code);
        return userService.register(registerDTO, code);
    }

    @PostMapping("/login")
    public Result login(LoginDTO loginDTO, HttpSession session) {
        String code = (String) session.getAttribute(RedisConstant.REGISTER_CAPTCHA_CODE_KEY);
        log.info("code :{}", code);
        return userService.login(loginDTO, code, session);
    }

    @PostMapping("/resetPwd")
    public Result resetPwd(ResetPwdDTO resetPwdDTO, HttpSession session) {
        String code = (String) session.getAttribute(RedisConstant.REGISTER_CAPTCHA_CODE_KEY);
        log.info("code :{}", code);
        return userService.resetPwd(resetPwdDTO, code);
    }

    // 这个方法返回的是头像的字节流放在Response里面
    @GetMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response, @PathVariable String userId) throws IOException {
        userService.getAvatar(response, userId);
    }

    @PostMapping("/updateUserAvatar")
    public Result updateUserAvatar(MultipartFile avatar) throws IOException {
        return userService.updateUserAvatar(avatar);
    }

    @PostMapping("/updatePassword")
    public Result updatePassword(String password) {
        return userService.updatePassword(password);
    }

    @PostMapping("/getUseSpace")
    public Result getUseSpace() {
        return userService.getUseSpace();
    }

    @PostMapping("/logout")
    public Result logout() {
        return userService.logout();
    }

}
