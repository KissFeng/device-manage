package chat.hbcit.devicemanagebackend.ai.tools;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
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
 * 管理员专属的运营分析、用户查询与对话式管理操作工具集（仅 ADMIN / ROOT 角色注入）
 */
public class AdminInsightTools {

    private final Long currentAdminId;
    private final DeviceService deviceService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final BorrowService borrowService;
    private final DeviceInstanceService deviceInstanceService;
    private final BorrowRecordMapper borrowRecordMapper;
    private final DeviceInstanceMapper deviceInstanceMapper;
    private final BorrowOverdueScheduler overdueScheduler;
    private final Consumer<AgentStepEvent> stepEmitter;

    public AdminInsightTools(Long currentAdminId,
                             DeviceService deviceService,
                             CategoryService categoryService,
                             UserService userService,
                             BorrowService borrowService,
                             DeviceInstanceService deviceInstanceService,
                             BorrowRecordMapper borrowRecordMapper,
                             DeviceInstanceMapper deviceInstanceMapper,
                             BorrowOverdueScheduler overdueScheduler,
                             Consumer<AgentStepEvent> stepEmitter) {
        this.currentAdminId = currentAdminId;
        this.deviceService = deviceService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.borrowService = borrowService;
        this.deviceInstanceService = deviceInstanceService;
        this.borrowRecordMapper = borrowRecordMapper;
        this.deviceInstanceMapper = deviceInstanceMapper;
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

    @Tool(description = "查询系统用户列表或管理员列表（自动脱敏隐藏密码），可按用户名模糊查询或按角色(USER/ADMIN/ROOT)筛选")
    public List<Map<String, Object>> listUsers(
            @ToolParam(required = false, description = "用户名关键词，为空则不限") String username,
            @ToolParam(required = false, description = "角色过滤：USER-普通用户, ADMIN-管理员, ROOT-超级管理员；为空查全部") String role) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String normalizedRole = (role != null && !role.isBlank()) ? role.trim().toUpperCase() : null;
        String cleanUsername = (username != null && !username.isBlank()) ? username.trim() : null;
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "search",
                "正在查询 用户列表" + (normalizedRole != null ? "（角色: " + normalizedRole + "）" : ""),
                null, null));

        PageResult<User> page = userService.getUserList(cleanUsername, normalizedRole, 1, 50);
        List<Map<String, Object>> list = page.getList().stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("role", u.getRole());
            item.put("status", u.getStatus());
            item.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "search",
                String.format("已查询 用户列表（共 %d 人）", list.size()),
                null, System.currentTimeMillis() - start));
        return list;
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

    @Tool(description = "审批借用申请（写操作：调用前须向管理员确认记录ID和审批动作），支持 APPROVE（通过并分配实例）或 REJECT（拒绝）")
    public Map<String, Object> approveBorrowApplication(
            @ToolParam(description = "借用记录ID") Long recordId,
            @ToolParam(description = "审批动作：APPROVE 或 REJECT") String action,
            @ToolParam(required = false, description = "审批备注说明") String remark) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String normalizedAction = action != null ? action.trim().toUpperCase() : "";
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "tool",
                String.format("正在执行 借用审批（记录 #%d -> %s）", recordId, normalizedAction),
                null, null));

        try {
            borrowService.approveBorrow(recordId, currentAdminId, normalizedAction, remark);
            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "done", "tool",
                    String.format("已完成 借用审批（记录 #%d 已%s）", recordId, "APPROVE".equals(normalizedAction) ? "批准" : "拒绝"),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", true, "recordId", recordId, "action", normalizedAction);
        } catch (Exception ex) {
            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "error", "alert",
                    "借用审批失败：" + ex.getMessage(),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    @Tool(description = "确认设备归还核验（写操作：调用前须向管理员确认记录ID），完成后自动释放物理设备实例占用状态")
    public Map<String, Object> confirmDeviceReturn(
            @ToolParam(description = "借用记录ID") Long recordId,
            @ToolParam(required = false, description = "归还核验备注，如：外观完好") String returnRemark) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "tool",
                String.format("正在确认 设备归还（记录 #%d）", recordId),
                null, null));

        try {
            borrowService.confirmReturn(recordId, returnRemark != null ? returnRemark : "设备完好归还");
            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "done", "tool",
                    String.format("已确认 归还核验（记录 #%d 已释放实例）", recordId),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", true, "recordId", recordId);
        } catch (Exception ex) {
            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "error", "alert",
                    "确认归还失败：" + ex.getMessage(),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", false, "message", ex.getMessage());
        }
    }

    @Tool(description = "按实例名称或位置模糊检索物理设备实例列表（获取 instanceId 用于状态管理）")
    public List<Map<String, Object>> searchDeviceInstances(
            @ToolParam(required = false, description = "物理实例名称关键词，例如 A7M4-001") String keyword) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "search", "正在检索 物理设备实例", null, null));

        LambdaQueryWrapper<DeviceInstance> wrapper = new LambdaQueryWrapper<>();
        if (kw != null) {
            wrapper.like(DeviceInstance::getInstanceName, kw);
        }
        wrapper.last("LIMIT 30");
        List<DeviceInstance> instances = deviceInstanceMapper.selectList(wrapper);

        List<Map<String, Object>> list = instances.stream().map(inst -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("instanceId", inst.getId());
            item.put("deviceId", inst.getDeviceId());
            item.put("instanceName", inst.getInstanceName());
            item.put("location", inst.getLocation());
            item.put("status", inst.getStatus());
            item.put("description", inst.getDescription());
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "search",
                String.format("已检索 物理设备实例（命中 %d 台）", list.size()),
                null, System.currentTimeMillis() - start));
        return list;
    }

    @Tool(description = "更新指定物理设备实例的健康状态（写操作：调用前须向管理员确认实例ID和目标状态），状态支持 NORMAL(正常)、MAINTENANCE(维保中)、SCRAPPED(已报废)")
    public Map<String, Object> updateDeviceInstanceStatus(
            @ToolParam(description = "物理设备实例ID（可先通过 searchDeviceInstances 获取）") Long instanceId,
            @ToolParam(description = "目标状态：NORMAL / MAINTENANCE / SCRAPPED") String status,
            @ToolParam(required = false, description = "状态变更备注或维保原因") String description) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String normalizedStatus = status != null ? status.trim().toUpperCase() : "";
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "tool",
                String.format("正在更新 实例状态（实例 #%d -> %s）", instanceId, normalizedStatus),
                null, null));

        try {
            if (DeviceStatus.fromCode(normalizedStatus) == null) {
                throw new IllegalArgumentException("无效状态，仅支持 NORMAL / MAINTENANCE / SCRAPPED");
            }
            DeviceInstance patch = new DeviceInstance();
            patch.setStatus(normalizedStatus);
            if (description != null && !description.isBlank()) {
                patch.setDescription(description.trim());
            }
            deviceInstanceService.updateInstance(instanceId, patch);

            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "done", "tool",
                    String.format("已更新 实例 #%d 状态为 %s", instanceId, normalizedStatus),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", true, "instanceId", instanceId, "status", normalizedStatus);
        } catch (Exception ex) {
            AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                    stepId, "error", "alert",
                    "更新实例状态失败：" + ex.getMessage(),
                    null, System.currentTimeMillis() - start));
            return Map.of("success", false, "message", ex.getMessage());
        }
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
