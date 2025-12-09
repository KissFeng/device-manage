package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员借用审批管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/admin/borrow")
public class AdminBorrowController {
    @Autowired
    private BorrowService borrowService;

    /**
     * 分页查询待审批申请列表
     *
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    @GetMapping("/pending")
    public Result<PageResult<BorrowRecordVO>> getPendingRecords(@RequestParam(required = false) String deviceName,
                                                               @RequestParam(required = false) String username,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "10") Integer size) {
        PageResult<BorrowRecordVO> result = borrowService.getPendingRecords(deviceName, username, page, size);
        return Result.success("获取成功", result);
    }

    /**
     * 审批借用申请
     *
     * @param id     借用记录ID
     * @param params 请求参数（包含action和remark）
     * @return 响应结果
     */
    @PutMapping("/{id}/approve")
    public Result<?> approveBorrow(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long approveUserId = Long.valueOf(authentication.getName());
        String action = params.get("action");
        String remark = params.get("remark");
        if (action == null || action.isEmpty()) {
            return Result.error(400, "操作类型不能为空");
        }
        borrowService.approveBorrow(id, approveUserId, action, remark);
        return Result.success("审批成功");
    }

    /**
     * 确认归还
     *
     * @param id     借用记录ID
     * @param params 请求参数（包含returnRemark）
     * @return 响应结果
     */
    @PutMapping("/{id}/return")
    public Result<?> confirmReturn(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String returnRemark = params.get("returnRemark");
        borrowService.confirmReturn(id, returnRemark);
        return Result.success("归还确认成功");
    }

    /**
     * 分页查询所有借用记录
     *
     * @param status     状态
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    @GetMapping("/records")
    public Result<PageResult<BorrowRecordVO>> getAllRecords(@RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String deviceName,
                                                          @RequestParam(required = false) String username,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        PageResult<BorrowRecordVO> result = borrowService.getAllRecords(status, deviceName, username, page, size);
        return Result.success("获取成功", result);
    }
}

