package com.xcu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcu.entity.dto.LoginDTO;
import com.xcu.entity.dto.RegisterDTO;
import com.xcu.entity.dto.ResetPwdDTO;
import com.xcu.entity.dto.SendEmailCodeDTO;
import com.xcu.entity.pojo.User;
import com.xcu.result.Result;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public interface UserService extends IService<User> {

    Result sendEmailCode(SendEmailCodeDTO sendEmailCodeDTO, String code);

    Result register(RegisterDTO registerDTO, String code);

    Result login(LoginDTO loginDTO, String code, HttpSession session);

    Result resetPwd(ResetPwdDTO resetPwdDTO, String code);

    void getAvatar(HttpServletResponse response, String userId) throws IOException;

    Result updateUserAvatar(MultipartFile avatar) throws IOException;

    Result updatePassword(String password);

    Result getUseSpace();

    Result logout();
}
