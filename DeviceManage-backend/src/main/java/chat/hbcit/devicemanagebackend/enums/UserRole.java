package chat.hbcit.devicemanagebackend.enums;

/**
 * 用户角色枚举
 *
 * @author DeviceManage
 */
public enum UserRole {
    /**
     * 超级管理员
     */
    ROOT("ROOT", "超级管理员"),

    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 普通用户
     */
    USER("USER", "普通用户");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
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
     * @param code 角色代码
     * @return 用户角色枚举
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 检查当前角色是否可以管理目标角色
     *
     * @param currentRole 当前用户角色
     * @param targetRole  目标用户角色
     * @return true 可以管理，false 不能管理
     */
    public static boolean canManage(String currentRole, String targetRole) {
        if (currentRole == null || targetRole == null) {
            return false;
        }

        // ROOT 可以管理所有角色
        if (ROOT.code.equals(currentRole)) {
            return true;
        }

        // ADMIN 只能管理 USER
        if (ADMIN.code.equals(currentRole)) {
            return USER.code.equals(targetRole);
        }

        // USER 不能管理任何角色
        return false;
    }
}

