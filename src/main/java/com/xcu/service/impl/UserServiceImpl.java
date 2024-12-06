package com.xcu.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcu.context.BaseContext;
import com.xcu.entity.vo.GetUseSpaceVO;
import com.xcu.exception.BaseException;
import com.xcu.constants.Constants;
import com.xcu.constants.RedisConstant;
import com.xcu.entity.dto.LoginDTO;
import com.xcu.entity.dto.RegisterDTO;
import com.xcu.entity.dto.ResetPwdDTO;
import com.xcu.entity.dto.SendEmailCodeDTO;
import com.xcu.entity.pojo.User;
import com.xcu.entity.vo.LoginVO;
import com.xcu.mapper.FileMapper;
import com.xcu.mapper.UserMapper;
import com.xcu.result.Result;
import com.xcu.service.UserService;
import com.xcu.util.EmailSendUtil;
import com.xcu.util.PasswordEncoder;
import com.xcu.util.RedisIdIncrement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final EmailSendUtil emailSendUtil;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserMapper userMapper;

    private final RedisIdIncrement redisIdIncrement;

    private final FileMapper fileMapper;

    @Override
    public Result sendEmailCode(SendEmailCodeDTO sendEmailCodeDTO, String captchaCode) {
        String email = sendEmailCodeDTO.getEmail();
        String checkCode = sendEmailCodeDTO.getCheckCode();
        Integer type = sendEmailCodeDTO.getType();

        if (captchaCode == null || !captchaCode.equalsIgnoreCase(checkCode)) {
            return Result.fail("图形验证码不正确");
        }

        // 在注册的时候注意一个邮箱只能使用一次
        if (type == 0) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user != null) {
                return Result.fail("这个邮箱不可重复使用");
            }
        }

        // 这个是邮箱验证码
        String code = RandomUtil.randomString(6);
        stringRedisTemplate.opsForValue().set(RedisConstant.LOGIN_CODE_KEY + email, code,
                RedisConstant.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        emailSendUtil.sendEmailCode(email, code);
        log.info("邮箱发送验证码 : {}", code);

        return Result.success();
    }

    @Override
    public Result register(RegisterDTO registerDTO, String captchaCode) {
        String email = registerDTO.getEmail();
        String nickName = registerDTO.getNickName();
        String password = registerDTO.getPassword();
        String checkCode = registerDTO.getCheckCode();
        String emailCode = registerDTO.getEmailCode();

        if (captchaCode == null || !captchaCode.equalsIgnoreCase(checkCode)) {
            return Result.fail("图形验证码不正确");
        }

        String code = stringRedisTemplate.opsForValue().get(RedisConstant.LOGIN_CODE_KEY + email);
        if (code == null || !code.equals(emailCode)) {
            return Result.fail("邮箱验证码不正确");
        }

        User oneUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getNickName, nickName));
        if (oneUser != null) {
            return Result.fail("用户名已存在");
        }

        // 这里的分配的云盘的大小单位是不确定的（以B为单位）
        Long userId = redisIdIncrement.nextId("user");
        User user = User.builder()
                .id(userId)
                .email(email)
                .nickName(nickName)
                .password(PasswordEncoder.encode(password))
                .usedSpace(0L)
                .totalSpace(Constants.totalSpace)
                .qqAvatar(Constants.FILE_ROOT_DIR + Constants.DEFAULT_AVATAR)
                .build();
        userMapper.insert(user);

        return Result.success();
    }

    @Override
    public Result login(LoginDTO loginDTO, String captchaCode, HttpSession session) {
        String email = loginDTO.getEmail();
        String password = loginDTO.getPassword();
        String checkCode = loginDTO.getCheckCode();

        if (captchaCode == null || !captchaCode.equalsIgnoreCase(checkCode)) {
            return Result.fail("图形验证码不正确");
        }

        // 密码加盐了，只需要传来原密码就行的,但是前端传来的是原密码经过md5加密过的
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null || !user.getPassword().equals("28yegq8utx88a7q4r23v@281e381a67e0e983d7d2221b3897c88a")) {
            return Result.fail("用户名或密码错误");
        }

        if (!user.getStatus()) {
            return Result.fail("这个用户被禁用了");
        }

        // 没有使用到令牌技术，所以只能把用户信息保存到session中
        session.setAttribute(Constants.ID, user.getId());

        // 后续还有qq登录，更新一些内容
        User userParam = User.builder()
                .email(email)
                .lastLoginTime(LocalDateTime.now())
                .build();
        userMapper.update(userParam, new LambdaQueryWrapper<User>().eq(User::getEmail, email));

        // 每次登录都要计算一下用户使用了多少空间
        stringRedisTemplate.opsForValue().set(RedisConstant.FILE_USEDSPACE_KEY + user.getId(),
                user.getUsedSpace().toString());

        LoginVO loginVO = new LoginVO(user.getNickName(), user.getId().toString(), null, false);
        return Result.success(loginVO);
    }

    /**
     * 这个是登录之前的重新设置密码
     * @param resetPwdDTO
     * @param captchaCode
     * @return
     */
    @Override
    public Result resetPwd(ResetPwdDTO resetPwdDTO, String captchaCode) {
        String email = resetPwdDTO.getEmail();
        String password = resetPwdDTO.getPassword();
        String emailCode = resetPwdDTO.getEmailCode();

        if (captchaCode == null || !captchaCode.equalsIgnoreCase(emailCode)) {
            return Result.fail("图形验证码不正确");
        }

        String code = stringRedisTemplate.opsForValue().get(RedisConstant.LOGIN_CODE_KEY + email);
        if (code == null || !code.equals(emailCode)) {
            return Result.fail("邮箱验证码不正确");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        user.setPassword(PasswordEncoder.encode(password));

        return Result.success();
    }

    /**
     * 从本地目录里返回具体的文件
     * @param response
     * @param userId
     */
    @Override
    public void getAvatar(HttpServletResponse response, String userId) throws IOException {
        Long id = BaseContext.getUserId();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, id));
        if (user == null || !user.getStatus()) {
            throw new BaseException("用户不存在或被禁用");
        }

        // 这里存储完整的云端地址，或是完整的本地地址
        String path = user.getQqAvatar();
        File file = new File(path);
        if (!file.exists()) {
            // 采取一个默认的头像
            file = new File(Constants.FILE_ROOT_DIR + Constants.DEFAULT_AVATAR);
        }

        try (
                // 创建 FileInputStream 并获取 FileChannel
                FileInputStream fileInputStream = new FileInputStream(file);
                FileChannel fileChannel = fileInputStream.getChannel();
                // 获取 HTTP 响应的 WritableByteChannel
                WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream())
        ) {
            // 使用 transferTo 高效传输文件数据
            long position = 0;
            long fileSize = fileChannel.size();
            while (position < fileSize) {
                position += fileChannel.transferTo(position, fileSize - position, outputChannel);
            }

            response.getOutputStream().flush();
        }
    }

    @Transactional
    @Override
    public Result updateUserAvatar(MultipartFile avatar) throws IOException {
        Long userId = BaseContext.getUserId();

        // 删除老的头像
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
        if (!user.getQqAvatar().equals(Constants.FILE_ROOT_DIR + Constants.DEFAULT_AVATAR)) {
            FileUtil.del(user.getQqAvatar());
        }

        // 设置新的头像
        String path = Constants.FILE_ROOT_DIR + Constants.AVATAR_DIR + UUID.fastUUID() + Constants.AVATAR_SUFFIX;
        File file = new File(path);
        avatar.transferTo(file);
        user.setQqAvatar(path);
        userMapper.updateById(user);

        return Result.success();
    }

    /**
     * 这个是登录之后的修改密码
     * @param password
     * @return
     */
    @Override
    public Result updatePassword(String password) {
        Long userId = BaseContext.getUserId();
        User userParam = User.builder()
                .id(userId)
                .password(PasswordEncoder.encode(password)) // 注意这里的是原密码
                .build();
        userMapper.updateById(userParam);
        return Result.success();
    }

    /**
     * 用户的使用空间在redis中也存一份,所以要时刻保持这两个数据的一致性
     * @return
     */
    @Override
    public Result getUseSpace() {
        Long userId = BaseContext.getUserId();
        User user = userMapper.selectById(userId);

        // Integer usedSpace = fileMapper.selectUserSpace(id);
        String s = stringRedisTemplate.opsForValue().get(RedisConstant.FILE_USEDSPACE_KEY + userId);
        Long usedSpace = Long.valueOf(s);
        Long totalSpace = user.getTotalSpace();

        return Result.success(new GetUseSpaceVO(usedSpace, totalSpace));
    }

    @Override
    public Result logout() {
        BaseContext.removeUser();
        return Result.success();
    }

    // 为了保证数据库与redis的一致性
    public void updateUsedSpace(long userId, long usedSpace) {
        User user = User.builder()
                .id(userId)
                .usedSpace(usedSpace)
                .build();
        userMapper.updateById(user);
        stringRedisTemplate.opsForValue().set(RedisConstant.FILE_USEDSPACE_KEY + userId, String.valueOf(usedSpace));
    }

}
