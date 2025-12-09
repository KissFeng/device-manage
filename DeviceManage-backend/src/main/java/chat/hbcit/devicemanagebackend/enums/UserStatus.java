package chat.hbcit.devicemanagebackend.enums;

/**
 * 用户状态枚举
 *
 * @author DeviceManage
 */
public enum UserStatus {
    /**
     * 激活
     */
    ACTIVE("ACTIVE", "激活"),

    /**
     * 禁用
     */
    INACTIVE("INACTIVE", "禁用");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 用户状态枚举
     */
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

