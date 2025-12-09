package chat.hbcit.devicemanagebackend.enums;

/**
 * 借用记录状态枚举
 *
 * @author DeviceManage
 */
public enum BorrowStatus {
    /**
     * 申请中（待审批）
     */
    PENDING("PENDING", "申请中"),

    /**
     * 已批准（已借出）
     */
    APPROVED("APPROVED", "已批准"),

    /**
     * 已拒绝
     */
    REJECTED("REJECTED", "已拒绝"),

    /**
     * 已归还
     */
    RETURNED("RETURNED", "已归还");

    private final String code;
    private final String description;

    BorrowStatus(String code, String description) {
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
     * @return 借用状态枚举
     */
    public static BorrowStatus fromCode(String code) {
        for (BorrowStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

