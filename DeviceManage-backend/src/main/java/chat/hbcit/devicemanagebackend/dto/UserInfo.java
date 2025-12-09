package chat.hbcit.devicemanagebackend.dto;

import lombok.Data;

/**
 * 用户信息DTO
 *
 * @author DeviceManage
 */
@Data
public class UserInfo {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色
     */
    private String role;
}

