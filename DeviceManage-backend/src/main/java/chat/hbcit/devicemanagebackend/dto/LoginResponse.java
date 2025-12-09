package chat.hbcit.devicemanagebackend.dto;

import lombok.Data;

/**
 * 登录响应DTO
 *
 * @author DeviceManage
 */
@Data
public class LoginResponse {
    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserInfo userInfo;
}

