package chat.hbcit.devicemanagebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借用记录实体类
 *
 * @author DeviceManage
 */
@Data
@TableName("borrow_records")
public class BorrowRecord {
    /**
     * 借用记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（借用人）
     */
    private Long userId;

    /**
     * 设备ID（保留用于兼容）
     */
    private Long deviceId;

    /**
     * 设备实例ID
     */
    private Long deviceInstanceId;

    /**
     * 借用数量（保留用于兼容，实际使用 deviceInstanceId）
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
     * 状态：PENDING-申请中，APPROVED-已批准，REJECTED-已拒绝，RETURNED-已归还
     */
    private String status;

    /**
     * 审批人ID（管理员）
     */
    private Long approveUserId;

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
     * 创建时间（申请时间）
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

