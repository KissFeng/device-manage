package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.dto.LoginRequest;
import chat.hbcit.devicemanagebackend.dto.LoginResponse;

/**
 * 认证服务接口
 *
 * @author DeviceManage
 */
public interface AuthService {
    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应（包含Token和用户信息）
     */
    LoginResponse login(LoginRequest request);
}

