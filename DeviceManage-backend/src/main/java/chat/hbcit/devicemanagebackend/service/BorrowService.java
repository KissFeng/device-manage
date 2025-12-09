package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;

/**
 * 借用服务接口
 *
 * @author DeviceManage
 */
public interface BorrowService {
    /**
     * 提交借用申请
     *
     * @param userId 用户ID
     * @param record 借用记录
     * @return 创建的借用记录
     */
    BorrowRecord applyBorrow(Long userId, BorrowRecord record);

    /**
     * 分页查询我的借用记录
     *
     * @param userId 用户ID
     * @param status 状态
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    PageResult<BorrowRecordVO> getMyRecords(Long userId, String status, Integer page, Integer size);

    /**
     * 分页查询待审批申请列表
     *
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    PageResult<BorrowRecordVO> getPendingRecords(String deviceName, String username, Integer page, Integer size);

    /**
     * 审批借用申请
     *
     * @param recordId 借用记录ID
     * @param approveUserId 审批人ID
     * @param action 操作（APPROVE/REJECT）
     * @param remark 审批备注
     */
    void approveBorrow(Long recordId, Long approveUserId, String action, String remark);

    /**
     * 确认归还
     *
     * @param recordId 借用记录ID
     * @param returnRemark 归还备注
     */
    void confirmReturn(Long recordId, String returnRemark);

    /**
     * 分页查询所有借用记录（管理员）
     *
     * @param status     状态
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    PageResult<BorrowRecordVO> getAllRecords(String status, String deviceName, String username, Integer page, Integer size);
}

