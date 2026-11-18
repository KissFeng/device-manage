package chat.hbcit.devicemanagebackend.ai.tools;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.service.UserService;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 管理员专属的运营分析与资讯查询工具集（仅 ADMIN / ROOT 角色注入）
 */
public class AdminInsightTools {

    private final DeviceService deviceService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final BorrowService borrowService;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowOverdueScheduler overdueScheduler;
    private final Consumer<AgentStepEvent> stepEmitter;

    public AdminInsightTools(DeviceService deviceService,
                             CategoryService categoryService,
                             UserService userService,
                             BorrowService borrowService,
                             BorrowRecordMapper borrowRecordMapper,
                             BorrowOverdueScheduler overdueScheduler,
                             Consumer<AgentStepEvent> stepEmitter) {
        this.deviceService = deviceService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.borrowService = borrowService;
        this.borrowRecordMapper = borrowRecordMapper;
        this.overdueScheduler = overdueScheduler;
        this.stepEmitter = stepEmitter;
    }

    @Tool(description = "获取平台全局运营概览统计：设备总数、分类总数、注册用户总数、待审批申请数、在借中数量及定时逾期巡检摘要")
    public Map<String, Object> getSystemOverviewStats() {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "tool", "正在汇总 系统全局资产与借用概览", null, null));

        long totalDevices = deviceService.getDeviceList(null, null, null, null, 1, 1).getTotal();
        int totalCategories = categoryService.getAllCategories().size();
        long totalUsers = userService.getUserList(null, null, 1, 1).getTotal();
        long pendingBorrows = borrowService.getPendingRecords(null, null, 1, 1).getTotal();
        long approvedBorrows = borrowService.getAllRecords(BorrowStatus.APPROVED.getCode(), null, null, 1, 1).getTotal();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDevices", totalDevices);
        stats.put("totalCategories", totalCategories);
        stats.put("totalUsers", totalUsers);
        stats.put("pendingBorrowCount", pendingBorrows);
        stats.put("activeBorrowCount", approvedBorrows);
        stats.put("overdueSummary", overdueScheduler != null ? overdueScheduler.getLatestSummary() : Map.of());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "tool",
                String.format("已汇总 系统概览（设备 %d 款，待审批 %d 条，在借 %d 条）", totalDevices, pendingBorrows, approvedBorrows),
                null, System.currentTimeMillis() - start));
        return stats;
    }

    @Tool(description = "查询当前待管理员审批的设备借用申请列表，支持按设备名称或申请人用户名筛选")
    public List<Map<String, Object>> listPendingBorrowApplications(
            @ToolParam(required = false, description = "设备名称关键词") String deviceName,
            @ToolParam(required = false, description = "申请人用户名关键词") String username) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "search", "正在查询 待审批借用申请", null, null));

        PageResult<BorrowRecordVO> page = borrowService.getPendingRecords(deviceName, username, 1, 20);
        List<Map<String, Object>> list = page.getList().stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("username", r.getUsername());
            item.put("realName", r.getRealName());
            item.put("deviceName", r.getDeviceName());
            item.put("purpose", r.getPurpose());
            item.put("expectedReturnDate", r.getExpectedReturnDate() != null ? r.getExpectedReturnDate().toString() : null);
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "search",
                String.format("已查询 待审批申请（共 %d 条）", list.size()),
                null, System.currentTimeMillis() - start));
        return list;
    }

    @Tool(description = "实时查询所有已批准但超过预计归还日期仍未归还的逾期借用记录")
    public List<Map<String, Object>> listOverdueBorrowRecords() {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "alert", "正在扫描 逾期未归还借用记录", null, null));

        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, BorrowStatus.APPROVED.getCode())
                .lt(BorrowRecord::getExpectedReturnDate, LocalDate.now());
        List<BorrowRecord> overdue = borrowRecordMapper.selectList(wrapper);

        List<Map<String, Object>> list = overdue.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("userId", r.getUserId());
            item.put("deviceId", r.getDeviceId());
            item.put("deviceInstanceId", r.getDeviceInstanceId());
            item.put("expectedReturnDate", r.getExpectedReturnDate() != null ? r.getExpectedReturnDate().toString() : null);
            item.put("purpose", r.getPurpose());
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "alert",
                String.format("已完成 逾期扫描（发现 %d 条逾期记录）", list.size()),
                null, System.currentTimeMillis() - start));
        return list;
    }

    @Tool(description = "按状态、设备名称或用户名查询全库借用历史记录")
    public List<Map<String, Object>> queryBorrowRecords(
            @ToolParam(required = false, description = "状态：PENDING/APPROVED/REJECTED/RETURNED") String status,
            @ToolParam(required = false, description = "设备名称关键词") String deviceName,
            @ToolParam(required = false, description = "申请人用户名关键词") String username) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "search", "正在检索 全库借用记录", null, null));

        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        PageResult<BorrowRecordVO> page = borrowService.getAllRecords(normalizedStatus, deviceName, username, 1, 20);
        List<Map<String, Object>> list = page.getList().stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("username", r.getUsername());
            item.put("realName", r.getRealName());
            item.put("deviceName", r.getDeviceName());
            item.put("instanceName", r.getDeviceInstanceName());
            item.put("status", r.getStatus());
            item.put("expectedReturnDate", r.getExpectedReturnDate() != null ? r.getExpectedReturnDate().toString() : null);
            item.put("actualReturnDate", r.getActualReturnDate() != null ? r.getActualReturnDate().toString() : null);
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "search",
                String.format("已检索 全库借用记录（命中 %d 条）", list.size()),
                null, System.currentTimeMillis() - start));
        return list;
    }
}
