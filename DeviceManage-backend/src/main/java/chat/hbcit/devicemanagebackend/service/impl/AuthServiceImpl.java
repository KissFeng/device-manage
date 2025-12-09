package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.dto.LoginRequest;
import chat.hbcit.devicemanagebackend.dto.LoginResponse;
import chat.hbcit.devicemanagebackend.dto.UserInfo;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.UserStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.UserMapper;
import chat.hbcit.devicemanagebackend.service.AuthService;
import chat.hbcit.devicemanagebackend.util.JwtUtil;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 *
 * @author DeviceManage
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            logger.warn("登录失败：用户不存在，用户名：{}", request.getUsername());
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("登录失败：密码错误，用户名：{}", request.getUsername());
            // 这里输出用户输入的密码，加密后的
            logger.warn("用户输入的密码：{}，加密后的密码：{}", request.getPassword(), user.getPassword());

            throw new BusinessException(ResponseCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 检查用户状态
        if (!UserStatus.ACTIVE.getCode().equals(user.getStatus())) {
            logger.warn("登录失败：用户已被禁用，用户名：{}", request.getUsername());
            throw new BusinessException(ResponseCode.FORBIDDEN, "用户已被禁用，请联系管理员");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setRole(user.getRole());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserInfo(userInfo);

        logger.info("用户登录成功，用户名：{}，角色：{}", user.getUsername(), user.getRole());
        return response;
    }
}

