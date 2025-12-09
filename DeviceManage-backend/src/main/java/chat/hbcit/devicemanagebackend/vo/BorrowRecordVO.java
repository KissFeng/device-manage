package chat.hbcit.devicemanagebackend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借用记录视图对象（包含关联信息）
 *
 * @author DeviceManage
 */
@Data
public class BorrowRecordVO {
    /**
     * 借用记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 设备实例ID
     */
    private Long deviceInstanceId;

    /**
     * 设备实例名称
     */
    private String deviceInstanceName;

    /**
     * 借用数量（保留用于兼容）
     */
    private Integer quantity;

    /**
     * 借用目的
     */
    private String purpose;

    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;

    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approveUserId;

    /**
     * 审批人用户名
     */
    private String approveUserName;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 审批备注
     */
    private String approveRemark;

    /**
     * 归还备注
     */
    private String returnRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

