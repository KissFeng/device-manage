package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "管理端-借用审批与逾期监控", description = "管理员分页查看借用记录、审批申请、确认归还及查看定时逾期巡检摘要")
@RestController
@RequestMapping("/admin/borrow")
public class AdminBorrowController {
    @Autowired
    private BorrowService borrowService;

    @Autowired(required = false)
    private BorrowOverdueScheduler borrowOverdueScheduler;

    /**
     * 分页查询待审批申请列表
     *
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    @Operation(summary = "分页查询待审批借用申请", description = "支持按设备名称与申请人用户名模糊过滤")
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
    @Operation(summary = "审批借用申请", description = "支持 APPROVE（通过并自动分配空闲实例）或 REJECT（拒绝）")
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
    @Operation(summary = "确认设备归还", description = "核验归还设备并释放对应物理实例占用状态")
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
    @Operation(summary = "分页查询所有借用记录", description = "管理员可按状态、设备名称、用户名查询全部历史借用记录")
    @GetMapping("/records")
    public Result<PageResult<BorrowRecordVO>> getAllRecords(@RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String deviceName,
                                                          @RequestParam(required = false) String username,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size) {
        PageResult<BorrowRecordVO> result = borrowService.getAllRecords(status, deviceName, username, page, size);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取逾期借用定时巡检摘要", description = "返回 @Scheduled 定时任务最近一次扫描的逾期未归还统计结果")
    @GetMapping("/overdue-summary")
    public Result<Map<String, Object>> getOverdueSummary() {
        Map<String, Object> summary = borrowOverdueScheduler != null
                ? borrowOverdueScheduler.getLatestSummary()
                : Map.of("overdueCount", 0, "status", "DISABLED");
        return Result.success("获取成功", summary);
    }
}

