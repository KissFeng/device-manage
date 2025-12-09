package chat.hbcit.devicemanagebackend.enums;

/**
 * 设备状态枚举
 *
 * @author DeviceManage
 */
public enum DeviceStatus {
    /**
     * 正常
     */
    NORMAL("NORMAL", "正常"),

    /**
     * 维修中
     */
    MAINTENANCE("MAINTENANCE", "维修中"),

    /**
     * 已报废
     */
    SCRAPPED("SCRAPPED", "已报废");

    private final String code;
    private final String description;

    DeviceStatus(String code, String description) {
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
     * @return 设备状态枚举
     */
    public static DeviceStatus fromCode(String code) {
        for (DeviceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

