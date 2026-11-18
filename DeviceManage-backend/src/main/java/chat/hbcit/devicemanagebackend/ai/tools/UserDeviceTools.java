package chat.hbcit.devicemanagebackend.ai.tools;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.impl.DeviceServiceImpl;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import chat.hbcit.devicemanagebackend.vo.DeviceInstanceVO;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 普通用户与管理员共用的设备借用只读查询工具集（基于 Spring AI @Tool）
 */
public class UserDeviceTools {

    private final Long currentUserId;
    private final CategoryService categoryService;
    private final DeviceServiceImpl deviceService;
    private final BorrowService borrowService;
    private final Consumer<AgentStepEvent> stepEmitter;

    public UserDeviceTools(Long currentUserId,
                           CategoryService categoryService,
                           DeviceServiceImpl deviceService,
                           BorrowService borrowService,
                           Consumer<AgentStepEvent> stepEmitter) {
        this.currentUserId = currentUserId;
        this.categoryService = categoryService;
        this.deviceService = deviceService;
        this.borrowService = borrowService;
        this.stepEmitter = stepEmitter;
    }

    @Tool(description = "获取光影资产系统中所有的媒体设备分类列表（如相机机身、专业镜头、灯光系统等）")
    public List<Map<String, Object>> listDeviceCategories() {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "folder", "正在查询 设备分类列表", null, null));

        List<Category> categories = categoryService.getAllCategories();
        List<Map<String, Object>> result = categories.stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("name", c.getName());
            item.put("description", c.getDescription());
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "folder",
                String.format("已列出 设备分类列表（共 %d 个分类）", result.size()),
                null, System.currentTimeMillis() - start));
        return result;
    }

    @Tool(description = "查询当前可以借用的设备型号与实时可用实例数量，支持按关键词和分类ID筛选")
    public List<Map<String, Object>> searchAvailableDevices(
            @ToolParam(required = false, description = "设备名称关键词，例如 索尼、镜头、灯光、稳定器；为空则查询全部") String keyword,
            @ToolParam(required = false, description = "设备分类ID；为空则不限分类") Long categoryId) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String kwDesc = (keyword != null && !keyword.isBlank()) ? "keyword=\"" + keyword.trim() + "\"" : "全部设备";
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "search", "正在检索 可借用设备 (" + kwDesc + ")", null, null));

        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        PageResult<DeviceVO> page = deviceService.getDeviceListWithInstances(
                cleanKeyword,
                null,
                null,
                categoryId,
                1,
                50
        );

        // 若按设备名称未命中且未指定分类，尝试按分类名称模糊匹配（如“相机”、“镜头”、“灯光”）
        if ((page.getList() == null || page.getList().isEmpty()) && cleanKeyword != null && categoryId == null) {
            Long matchedCategoryId = categoryService.getAllCategories().stream()
                    .filter(c -> c.getName() != null && c.getName().contains(cleanKeyword))
                    .map(Category::getId)
                    .findFirst()
                    .orElse(null);
            if (matchedCategoryId != null) {
                page = deviceService.getDeviceListWithInstances(null, null, null, matchedCategoryId, 1, 50);
            }
        }

        List<Map<String, Object>> available = page.getList().stream()
                .filter(d -> DeviceStatus.NORMAL.getCode().equals(d.getOverallStatus()))
                .map(d -> {
                    List<DeviceInstanceVO> instances = d.getInstances() == null ? List.of() : d.getInstances();
                    long freeCount = instances.stream()
                            .filter(i -> DeviceStatus.NORMAL.getCode().equals(i.getStatus()) && !Boolean.TRUE.equals(i.getBorrowed()))
                            .count();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", d.getId());
                    item.put("name", d.getName());
                    item.put("model", d.getModel());
                    item.put("categoryName", d.getCategoryName());
                    item.put("totalInstances", instances.size());
                    item.put("availableCount", freeCount);
                    item.put("description", d.getDescription());
                    return item;
                })
                .filter(m -> ((Long) m.get("availableCount")) > 0)
                .collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "search",
                String.format("已检索 可借用设备（%s，命中 %d 款有库存设备）", kwDesc, available.size()),
                null, System.currentTimeMillis() - start));
        return available;
    }

    @Tool(description = "查询当前登录用户自己的设备借用申请记录与审批状态，可按状态(PENDING/APPROVED/REJECTED/RETURNED)筛选")
    public List<Map<String, Object>> getMyBorrowRecords(
            @ToolParam(required = false, description = "状态过滤：PENDING-待审批, APPROVED-借用中, REJECTED-已拒绝, RETURNED-已归还；为空查全部") String status) {
        String stepId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "running", "tool", "正在查询 我的借用记录", null, null));

        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        PageResult<BorrowRecordVO> page = borrowService.getMyRecords(currentUserId, normalizedStatus, 1, 20);
        List<Map<String, Object>> records = page.getList().stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("deviceName", r.getDeviceName());
            item.put("deviceModel", r.getDeviceModel());
            item.put("instanceName", r.getDeviceInstanceName());
            item.put("status", r.getStatus());
            item.put("purpose", r.getPurpose());
            item.put("expectedReturnDate", r.getExpectedReturnDate() != null ? r.getExpectedReturnDate().toString() : null);
            item.put("actualReturnDate", r.getActualReturnDate() != null ? r.getActualReturnDate().toString() : null);
            item.put("approveRemark", r.getApproveRemark());
            return item;
        }).collect(Collectors.toList());

        AgentStepEvent.emit(stepEmitter, new AgentStepEvent(
                stepId, "done", "tool",
                String.format("已读取 我的借用记录（共 %d 条）", records.size()),
                null, System.currentTimeMillis() - start));
        return records;
    }
}
